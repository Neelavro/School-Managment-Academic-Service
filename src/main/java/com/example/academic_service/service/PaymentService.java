package com.example.academic_service.service;

import com.example.academic_service.dto.*;
import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Online payment orchestrator (SSLCommerz).
 *
 * Flow:
 *   1. initOnlinePayment(invoiceId) — creates a Payment row (INITIATED),
 *      calls SSLCommerz Init API, returns the gateway redirect URL.
 *   2. handleIpn(formData)         — SSLCommerz webhooks here. We re-validate
 *      via the Validation API, then:
 *         success → mark payment SUCCESS, post two journal entries, update invoice
 *         failure → MOVE the row from payments → payment_failures
 *   3. verifyPayment(paymentId)    — same logic, but admin-triggered for stuck rows.
 *
 * Concurrency: a payment can only transition INITIATED → SUCCESS once (status
 * check). Idempotency: SSLCommerz sometimes retries the IPN; the second call
 * sees status=SUCCESS and returns immediately without double-posting.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final PaymentFailureRepository failureRepo;
    private final InvoiceRepository invoiceRepo;
    private final InvoiceLineRepository invoiceLineRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final AccountingSettingsService settingsService;
    private final JournalEntryService journalService;
    private final SslCommerzClient ssl;

    private final ObjectMapper json = new ObjectMapper();

    @Value("${app.public-base-url-frontend:http://localhost:9000}")
    private String publicFrontendBaseUrl;

    @Value("${app.public-base-url-backend:http://localhost:8084}")
    private String publicBackendBaseUrl;

    // ── Init ──────────────────────────────────────────────────────────────

    @Transactional
    public PaymentInitResponse initOnlinePayment(PaymentInitRequest req,
                                                  String initiatedBy, String ipAddress) {
        if (req.getInvoiceId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invoiceId is required");
        }

        // Settings must be fully configured for online payment.
        AccountingSettings settings = settingsService.getRequiredForOnlinePayment();

        Invoice invoice = invoiceRepo.findById(req.getInvoiceId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice is cancelled");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice is already fully paid");
        }
        BigDecimal outstanding = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nothing outstanding to pay");
        }

        // Resolve payer info — request overrides, fall back to enrollment.
        Enrollment enrollment = enrollmentRepo.findById(invoice.getEnrollmentId()).orElse(null);
        String payerName = firstNonBlank(req.getPayerName(),
                enrollment != null && enrollment.getStudent() != null
                        ? enrollment.getStudent().getNameEnglish() : null);
        String payerEmail = firstNonBlank(req.getPayerEmail(), "noreply@example.com");
        String payerPhone = firstNonBlank(req.getPayerPhone(), "01700000000");

        // Build our tran_id (also used as the natural idempotency key).
        String tranId = "TXN-" + invoice.getInvoiceNumber() + "-" + System.currentTimeMillis();

        // Persist Payment row BEFORE calling gateway so we never lose the audit trail.
        Payment payment = new Payment();
        payment.setTranId(tranId);
        payment.setInvoiceId(invoice.getId());
        payment.setMethod(PaymentMethod.ONLINE_SSLCOMMERZ);
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setAmount(outstanding);
        payment.setPayerName(payerName);
        payment.setPayerEmail(payerEmail);
        payment.setPayerPhone(payerPhone);
        payment.setInitiatedBy(initiatedBy);
        payment.setInitiatedIp(ipAddress);
        Payment savedPayment = paymentRepo.save(payment);

        // Build SSLCommerz init request.
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("store_id", ssl.getStoreId());
        fields.put("store_passwd", ssl.getStorePassword());
        fields.put("total_amount", outstanding.setScale(2, RoundingMode.HALF_UP).toPlainString());
        fields.put("currency", "BDT");
        fields.put("tran_id", tranId);
        fields.put("success_url", publicFrontendBaseUrl + "/payments/result?status=success&tranId=" + tranId);
        fields.put("fail_url",    publicFrontendBaseUrl + "/payments/result?status=fail&tranId=" + tranId);
        fields.put("cancel_url",  publicFrontendBaseUrl + "/payments/result?status=cancel&tranId=" + tranId);
        fields.put("ipn_url",     publicBackendBaseUrl + "/api/payments/ipn");
        fields.put("emi_option", "0");
        fields.put("cus_name", payerName);
        fields.put("cus_email", payerEmail);
        fields.put("cus_phone", payerPhone);
        fields.put("cus_add1", "N/A");
        fields.put("cus_city", "Dhaka");
        fields.put("cus_country", "Bangladesh");
        fields.put("shipping_method", "NO");
        fields.put("product_name", "School fees — " + invoice.getInvoiceNumber());
        fields.put("product_category", "Education");
        fields.put("product_profile", "general");
        fields.put("value_a", String.valueOf(savedPayment.getId())); // we get this back in the IPN

        JsonNode response = ssl.createSession(fields);
        String gatewayUrl = response.path("GatewayPageURL").asText(null);
        if (gatewayUrl == null || gatewayUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "SSLCommerz did not return a GatewayPageURL");
        }

        return new PaymentInitResponse(savedPayment.getId(), tranId, gatewayUrl);
    }

    // ── IPN ───────────────────────────────────────────────────────────────

    /**
     * Public webhook handler. SSLCommerz sends form-encoded data here.
     * Must be idempotent and tolerate replays.
     */
    @Transactional
    public void handleIpn(Map<String, String> formData) {
        String tranId = formData.get("tran_id");
        String valId = formData.get("val_id");
        if (tranId == null) return; // silently ignore malformed IPN

        // If we've already moved this to failures, IPN is replaying an old failure — ignore.
        if (failureRepo.existsByTranId(tranId)) return;

        Optional<Payment> opt = paymentRepo.findByTranId(tranId);
        if (opt.isEmpty()) return; // unknown — could be an orphaned ping
        Payment payment = opt.get();

        // Idempotency: if already SUCCESS, return (SSLCommerz retries IPN sometimes).
        if (payment.getStatus() == PaymentStatus.SUCCESS) return;

        if (valId == null || valId.isBlank()) {
            // No val_id means we can't validate; treat as failure.
            moveToFailures(payment, PaymentStatus.FAILED, "IPN missing val_id", asJson(formData));
            return;
        }

        applyValidation(payment, valId, asJson(formData));
    }

    // ── In-Person Cash Payment ────────────────────────────────────────────

    /**
     * Records an in-person cash payment received at the school's accounts desk.
     * Synchronous — no gateway round-trip, status is SUCCESS immediately.
     *
     * Validations:
     *   - Invoice must exist, not be CANCELLED, not be already PAID
     *   - amount must be > 0 and <= outstanding
     *   - Cash account must be configured in Accounting Settings
     *
     * Journal posting:
     *   - Dr Cash in Hand
     *   - Cr AR — Students
     *
     * Platform fee: intentionally NOT applied to cash payments. The SaaS
     * platform owner's commission is only triggered on gateway-routed
     * transactions (Phase 5 SSLCommerz path).
     */
    @Transactional
    public PaymentResponse recordCashPayment(com.example.academic_service.dto.CashPaymentRequest req,
                                              String receivedBy, String ipAddress) {
        if (req.getInvoiceId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invoiceId is required");
        }
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be > 0");
        }

        AccountingSettings settings = settingsService.getRequiredForCashPayment();

        Invoice invoice = invoiceRepo.findById(req.getInvoiceId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice is cancelled");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice is already fully paid");
        }
        BigDecimal outstanding = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
        if (req.getAmount().compareTo(outstanding) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Amount exceeds outstanding (" + outstanding + ")");
        }

        Enrollment enrollment = enrollmentRepo.findById(invoice.getEnrollmentId()).orElse(null);
        String payerName = firstNonBlank(req.getPayerName(),
                enrollment != null && enrollment.getStudent() != null
                        ? enrollment.getStudent().getNameEnglish() : null);

        String tranId = "CASH-" + invoice.getInvoiceNumber() + "-" + System.currentTimeMillis();

        // Create the payment row directly as SUCCESS.
        Payment payment = new Payment();
        payment.setTranId(tranId);
        payment.setInvoiceId(invoice.getId());
        payment.setMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setAmount(req.getAmount());
        payment.setPayerName(payerName);
        payment.setInitiatedBy(receivedBy);
        payment.setInitiatedIp(ipAddress);
        payment.setCompletedAt(LocalDateTime.now());
        Payment saved = paymentRepo.save(payment);

        // Post journal: Dr Cash / Cr AR.
        String narration = req.getNarration() != null && !req.getNarration().isBlank()
                ? req.getNarration()
                : "Cash received — " + invoice.getInvoiceNumber()
                  + (payerName != null ? " from " + payerName : "");

        JournalEntryRequest jReq = buildTwoLine(
                LocalDate.now(),
                JournalReferenceType.PAYMENT_CASH,
                saved.getId(),
                narration,
                settings.getCashAccountId(), req.getAmount(),
                "Cash received at desk — " + tranId,
                settings.getArAccountId(), req.getAmount(),
                "AR settle — " + invoice.getInvoiceNumber()
        );
        JournalEntryResponse je = journalService.post(jReq, receivedBy);
        saved.setMainJournalEntryId(je.getId());

        // Platform commission also applies to cash payments — the school owes
        // the platform whether the money came through SSLCommerz or the front
        // desk. Posts only if percent/flat are configured AND both fee
        // accounts are set in Accounting Settings.
        BigDecimal feeAmount = computePlatformFee(req.getAmount(), settings);
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0
                && settings.getPlatformFeeAccountId() != null
                && settings.getPlatformPayableAccountId() != null) {
            JournalEntryRequest feeEntry = buildTwoLine(
                    LocalDate.now(),
                    JournalReferenceType.PAYMENT_CASH,
                    saved.getId(),
                    "Platform commission — " + tranId,
                    settings.getPlatformFeeAccountId(), feeAmount,
                    "Platform fee expense",
                    settings.getPlatformPayableAccountId(), feeAmount,
                    "Payable to platform owner"
            );
            JournalEntryResponse feeJe = journalService.post(feeEntry, receivedBy);
            saved.setFeeJournalEntryId(feeJe.getId());
            saved.setPlatformFeeAmount(feeAmount);
        }
        paymentRepo.save(saved);

        // Update invoice paid amount + status.
        BigDecimal newPaid = invoice.getPaidAmount().add(req.getAmount());
        invoice.setPaidAmount(newPaid);
        invoice.setStatus(newPaid.compareTo(invoice.getTotalAmount()) >= 0
                ? InvoiceStatus.PAID : InvoiceStatus.PARTIAL);
        invoiceRepo.save(invoice);

        return hydrate(saved);
    }

    // ── Admin tools ───────────────────────────────────────────────────────

    /**
     * Admin marks a stuck INITIATED payment as failed. Useful when the parent
     * abandoned the gateway and no IPN ever came — frees the invoice for a fresh
     * payment attempt. Refuses if the payment already succeeded.
     */
    @Transactional
    public void markStuckAsFailed(Long paymentId, String reason) {
        Payment payment = paymentRepo.findById(paymentId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot mark a successful payment as failed");
        }
        moveToFailures(payment, PaymentStatus.FAILED,
                reason != null && !reason.isBlank() ? reason : "Marked as failed by admin",
                null);
    }

    // ── Reads ─────────────────────────────────────────────────────────────

    public Page<PaymentResponse> search(PaymentStatus status, PaymentMethod method,
                                         Long invoiceId, LocalDateTime from, LocalDateTime to,
                                         int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));
        return paymentRepo.search(status, method, invoiceId, from, to, pageable).map(this::hydrate);
    }

    public PaymentResponse getOne(Long id) {
        Payment p = paymentRepo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        return hydrate(p);
    }

    public Page<PaymentFailureResponse> searchFailures(PaymentStatus status, Long invoiceId,
                                                       LocalDateTime from, LocalDateTime to,
                                                       int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));
        return failureRepo.search(status, invoiceId, from, to, pageable).map(this::hydrateFailure);
    }

    public PaymentFailureResponse getFailure(Long id) {
        PaymentFailure f = failureRepo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment failure not found"));
        return hydrateFailure(f);
    }

    // ── Core: validation + posting ────────────────────────────────────────

    /**
     * Calls SSLCommerz Validation API and either marks SUCCESS + posts journals
     * or moves the row to failures.
     */
    private void applyValidation(Payment payment, String valId, String ipnRaw) {
        JsonNode validation;
        try {
            validation = ssl.validate(valId);
        } catch (Exception e) {
            moveToFailures(payment, PaymentStatus.FAILED,
                    "Validation API call failed: " + e.getMessage(), ipnRaw);
            return;
        }

        String status = validation.path("status").asText("");
        boolean ok = "VALID".equalsIgnoreCase(status) || "VALIDATED".equalsIgnoreCase(status);
        if (!ok) {
            String reason = "Gateway status=" + status
                    + (validation.has("error") ? " err=" + validation.path("error").asText() : "");
            moveToFailures(payment, "CANCELLED".equalsIgnoreCase(status)
                    ? PaymentStatus.CANCELLED : PaymentStatus.FAILED,
                    reason, validation.toString());
            return;
        }

        // Amount sanity check — gateway must report what we asked for.
        BigDecimal gatewayAmount;
        try {
            gatewayAmount = new BigDecimal(validation.path("amount").asText("0"));
        } catch (NumberFormatException e) {
            gatewayAmount = BigDecimal.ZERO;
        }
        if (gatewayAmount.compareTo(payment.getAmount()) != 0) {
            moveToFailures(payment, PaymentStatus.FAILED,
                    "Amount mismatch — expected " + payment.getAmount()
                            + " got " + gatewayAmount, validation.toString());
            return;
        }

        // Currency check.
        String currency = validation.path("currency").asText("");
        if (!"BDT".equalsIgnoreCase(currency)) {
            moveToFailures(payment, PaymentStatus.FAILED,
                    "Unexpected currency: " + currency, validation.toString());
            return;
        }

        // Store id check.
        String returnedStore = validation.path("store_id").asText("");
        if (!ssl.getStoreId().equalsIgnoreCase(returnedStore)) {
            moveToFailures(payment, PaymentStatus.FAILED,
                    "Store id mismatch: " + returnedStore, validation.toString());
            return;
        }

        // Apply success.
        markSuccess(payment, validation);
    }

    /**
     * Posts journal entries, updates invoice + payment row.
     * Locked behind the @Transactional caller.
     */
    private void markSuccess(Payment payment, JsonNode validation) {
        AccountingSettings settings = settingsService.getRequiredForOnlinePayment();

        Invoice invoice = invoiceRepo.findById(payment.getInvoiceId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invoice gone"));

        BigDecimal amount = payment.getAmount();
        BigDecimal feeAmount = computePlatformFee(amount, settings);

        // 1) Receipt entry: Dr Gateway Clearing / Cr AR
        JournalEntryRequest receipt = buildTwoLine(
                LocalDate.now(),
                JournalReferenceType.PAYMENT_ONLINE,
                payment.getId(),
                "Online payment received — " + invoice.getInvoiceNumber() + " (tran " + payment.getTranId() + ")",
                settings.getGatewayClearingAccountId(), amount,
                "Online gateway clearing — " + payment.getTranId(),
                settings.getArAccountId(), amount,
                "AR settle — " + invoice.getInvoiceNumber()
        );
        JournalEntryResponse mainJe = journalService.post(receipt, "system:ipn");
        payment.setMainJournalEntryId(mainJe.getId());

        // 2) Platform fee entry: Dr Platform Fee Expense / Cr Payable to Platform
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            JournalEntryRequest feeEntry = buildTwoLine(
                    LocalDate.now(),
                    JournalReferenceType.PAYMENT_ONLINE,
                    payment.getId(),
                    "Platform commission — " + payment.getTranId(),
                    settings.getPlatformFeeAccountId(), feeAmount,
                    "Platform fee expense",
                    settings.getPlatformPayableAccountId(), feeAmount,
                    "Payable to platform owner"
            );
            JournalEntryResponse feeJe = journalService.post(feeEntry, "system:ipn");
            payment.setFeeJournalEntryId(feeJe.getId());
            payment.setPlatformFeeAmount(feeAmount);
        }

        // 3) Update invoice paid_amount + status.
        BigDecimal newPaid = invoice.getPaidAmount().add(amount);
        invoice.setPaidAmount(newPaid);
        if (newPaid.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIAL);
        }
        invoiceRepo.save(invoice);

        // 4) Stamp success on the payment row.
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setCompletedAt(LocalDateTime.now());
        payment.setGatewayTranId(validation.path("val_id").asText(null));
        payment.setGatewayCardType(validation.path("card_type").asText(null));
        payment.setGatewayResponse(validation.toString());
        paymentRepo.save(payment);
    }

    /**
     * Copies the payment row into payment_failures with the failure metadata,
     * then deletes the original row from payments. Atomic within the @Transactional caller.
     */
    private void moveToFailures(Payment payment, PaymentStatus failureStatus,
                                  String reason, String gatewayRaw) {
        PaymentFailure f = new PaymentFailure();
        f.setTranId(payment.getTranId());
        f.setInvoiceId(payment.getInvoiceId());
        f.setMethod(payment.getMethod());
        f.setStatus(failureStatus);
        f.setAmount(payment.getAmount());
        f.setGatewayTranId(payment.getGatewayTranId());
        f.setGatewayCardType(payment.getGatewayCardType());
        f.setGatewayResponse(gatewayRaw != null ? gatewayRaw : payment.getGatewayResponse());
        f.setFailureReason(reason);
        f.setPayerName(payment.getPayerName());
        f.setPayerEmail(payment.getPayerEmail());
        f.setPayerPhone(payment.getPayerPhone());
        f.setInitiatedBy(payment.getInitiatedBy());
        f.setInitiatedIp(payment.getInitiatedIp());
        f.setInitiatedAt(payment.getInitiatedAt());
        f.setFailedAt(LocalDateTime.now());
        failureRepo.save(f);
        paymentRepo.delete(payment);
    }

    /**
     * platformFeePercent% of amount + platformFeeFlat. Rounded HALF_UP to 2dp.
     * Returns zero if both percent and flat are null/zero.
     */
    private BigDecimal computePlatformFee(BigDecimal amount, AccountingSettings settings) {
        BigDecimal percent = settings.getPlatformFeePercent() != null
                ? settings.getPlatformFeePercent() : BigDecimal.ZERO;
        BigDecimal flat = settings.getPlatformFeeFlat() != null
                ? settings.getPlatformFeeFlat() : BigDecimal.ZERO;
        BigDecimal percentAmt = amount.multiply(percent)
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return percentAmt.add(flat).setScale(2, RoundingMode.HALF_UP);
    }

    private JournalEntryRequest buildTwoLine(LocalDate date, JournalReferenceType refType,
                                              Long refId, String description,
                                              Long debitAccountId, BigDecimal debitAmount, String debitDesc,
                                              Long creditAccountId, BigDecimal creditAmount, String creditDesc) {
        JournalEntryRequest j = new JournalEntryRequest();
        j.setEntryDate(date);
        j.setReferenceType(refType);
        j.setReferenceId(refId);
        j.setDescription(description);
        List<JournalLineRequest> lines = new ArrayList<>(2);
        JournalLineRequest dr = new JournalLineRequest();
        dr.setAccountId(debitAccountId);
        dr.setDebit(debitAmount);
        dr.setDescription(debitDesc);
        lines.add(dr);
        JournalLineRequest cr = new JournalLineRequest();
        cr.setAccountId(creditAccountId);
        cr.setCredit(creditAmount);
        cr.setDescription(creditDesc);
        lines.add(cr);
        j.setLines(lines);
        return j;
    }

    private PaymentResponse hydrate(Payment p) {
        Invoice inv = invoiceRepo.findById(p.getInvoiceId()).orElse(null);
        String invoiceNumber = inv != null ? inv.getInvoiceNumber() : null;
        String studentName = null;
        if (inv != null) {
            Enrollment e = enrollmentRepo.findById(inv.getEnrollmentId()).orElse(null);
            if (e != null && e.getStudent() != null) studentName = e.getStudent().getNameEnglish();
        }
        return PaymentResponse.from(p, invoiceNumber, studentName);
    }

    private PaymentFailureResponse hydrateFailure(PaymentFailure f) {
        Invoice inv = invoiceRepo.findById(f.getInvoiceId()).orElse(null);
        String invoiceNumber = inv != null ? inv.getInvoiceNumber() : null;
        String studentName = null;
        if (inv != null) {
            Enrollment e = enrollmentRepo.findById(inv.getEnrollmentId()).orElse(null);
            if (e != null && e.getStudent() != null) studentName = e.getStudent().getNameEnglish();
        }
        return PaymentFailureResponse.from(f, invoiceNumber, studentName);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    private String asJson(Map<String, String> map) {
        try { return json.writeValueAsString(map); }
        catch (Exception e) { return map.toString(); }
    }
}
