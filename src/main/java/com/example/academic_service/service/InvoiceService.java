package com.example.academic_service.service;

import com.example.academic_service.dto.*;
import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Invoice lifecycle + monthly generation orchestrator.
 *
 * Generation:
 *   - One transaction PER INVOICE (REQUIRES_NEW), so a failure on one student
 *     does not roll back others. Failures are reported as warnings; the user
 *     can fix the data and re-run Generate — already-invoiced students are
 *     skipped automatically.
 *   - Enrollments are filtered IN THE DATABASE via findInvoiceCandidates(...)
 *     so we don't load the whole enrollment table into memory.
 *   - "Already invoiced" check ignores CANCELLED invoices, so regenerating
 *     after a cancellation just works.
 */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final DateTimeFormatter MONTH_TAG = DateTimeFormatter.ofPattern("yyyyMM");

    private final InvoiceRepository invoiceRepo;
    private final InvoiceLineRepository lineRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final FeeCategoryRepository feeCategoryRepo;
    private final FeePricingRepository pricingRepo;
    private final AccountingSettingsService settingsService;
    private final JournalEntryService journalService;
    private final AcademicYearRepository academicYearRepo;
    private final InvoiceGenerationProgressTracker progressTracker;
    private final PlatformFeeService platformFeeService;

    /**
     * Self-injected lazy proxy so we can call our own @Transactional method
     * from inside the orchestrator loop and actually go through the Spring
     * proxy (direct this.createOneInvoice would bypass the transaction).
     */
    @Autowired
    @Lazy
    private InvoiceService self;

    // ── Read ──────────────────────────────────────────────────────────────

    public Page<InvoiceResponse> search(Long enrollmentId, LocalDate period, InvoiceStatus status,
                                         Integer classId, Integer academicYearId, Integer shiftId,
                                         Integer genderSectionId, String studentSearch,
                                         String invoiceNumber,
                                         int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));
        LocalDate normalized = period != null ? period.withDayOfMonth(1) : null;
        String trimmedSearch = (studentSearch == null || studentSearch.isBlank()) ? null : studentSearch.trim();
        String trimmedInvNo  = (invoiceNumber == null || invoiceNumber.isBlank()) ? null : invoiceNumber.trim();
        Page<Invoice> invoices = invoiceRepo.search(
                enrollmentId, normalized, status,
                classId, academicYearId, shiftId, genderSectionId, trimmedSearch,
                trimmedInvNo,
                pageable);
        if (invoices.isEmpty()) return invoices.map(i -> InvoiceResponse.from(i, null, null, null, List.of()));
        return invoices.map(this::hydrate);
    }

    public InvoiceResponse getOne(Long id) {
        Invoice inv = invoiceRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        return hydrate(inv);
    }

    // ── Cancel ────────────────────────────────────────────────────────────

    /**
     * Cancels an invoice and posts a reversing journal entry. Only PENDING invoices
     * can be cancelled. (Once payments are linked, cancellation requires un-applying them.)
     */
    @Transactional
    public InvoiceResponse cancel(Long id, String reason, String user) {
        Invoice inv = invoiceRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        if (inv.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice already cancelled");
        }
        if (inv.getStatus() != InvoiceStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only PENDING invoices can be cancelled (status: " + inv.getStatus() + ")");
        }
        if (inv.getJournalEntryId() != null) {
            journalService.reverse(inv.getJournalEntryId(), "Invoice " + inv.getInvoiceNumber() + " cancelled"
                    + (reason != null && !reason.isBlank() ? ": " + reason : ""), user);
        }
        inv.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepo.save(inv);
        return hydrate(inv);
    }

    // ── Generate ──────────────────────────────────────────────────────────

    /**
     * Orchestrator — DELIBERATELY NOT @Transactional.
     * Each invoice is created in its own REQUIRES_NEW transaction so a single
     * failure does not roll back the whole batch.
     */
    /**
     * Kick off invoice generation in a background thread and return a
     * progress handle immediately. The frontend polls
     * GET /api/accounting/invoices/generate/progress/{taskId}
     * every ~500ms and renders a real progress bar based on processed/total.
     */
    public InvoiceGenerationProgress generateAsync(InvoiceGenerationRequest req, String generatedBy) {
        InvoiceGenerationProgress task = progressTracker.start(0);
        final String taskId = task.getTaskId();
        Thread t = new Thread(() -> {
            try {
                InvoiceGenerationResult result = generate(req, generatedBy, taskId);
                progressTracker.complete(taskId, result);
            } catch (Exception ex) {
                String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                progressTracker.fail(taskId, msg);
            }
        }, "invoice-gen-" + taskId);
        t.setDaemon(true);
        t.start();
        return task;
    }

    public InvoiceGenerationResult generate(InvoiceGenerationRequest req, String generatedBy) {
        return generate(req, generatedBy, null);
    }

    public InvoiceGenerationResult generate(InvoiceGenerationRequest req, String generatedBy, String taskId) {
        if (req.getBillingPeriod() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "billingPeriod is required");
        }
        LocalDate period = req.getBillingPeriod().withDayOfMonth(1);

        // Default academicYearId to the current active year when the caller
        // didn't pass one. Generation should never silently span across years.
        if (req.getAcademicYearId() == null) {
            req.setAcademicYearId(
                    academicYearRepo.findFirstByIsActiveTrue()
                            .map(AcademicYear::getId)
                            .orElse(null));
        }

        // Settings + AR account.
        AccountingSettings settings = settingsService.getRequiredForPosting();
        Long arAccountId = settings.getArAccountId();
        int dueDays = settings.getInvoiceDueDays() != null ? settings.getInvoiceDueDays() : 7;
        String invoicePrefix = settings.getInvoiceNumberPrefix() != null
                ? settings.getInvoiceNumberPrefix() : "INV";
        LocalDate issuedDate = LocalDate.now();
        LocalDate dueDate = issuedDate.plusDays(dueDays);

        // Active fee categories.
        List<FeeCategory> activeCategories = feeCategoryRepo.findByIsActive(true);
        if (activeCategories.isEmpty()) {
            InvoiceGenerationResult empty = new InvoiceGenerationResult(period, 0, 0, 0, 0, new ArrayList<>());
            empty.getWarnings().add("No active fee categories — nothing to invoice");
            return empty;
        }

        // Candidate enrollments — pushed to the DB.
        List<Integer> classIds = req.getClassIds() != null ? req.getClassIds() : List.of();
        boolean classIdsEmpty = classIds.isEmpty();
        List<Enrollment> candidates = enrollmentRepo.findInvoiceCandidates(
                req.getAcademicYearId(),
                classIdsEmpty,
                classIdsEmpty ? List.of(-1) : classIds  // dummy non-empty list satisfies JPQL IN
        );

        if (candidates.isEmpty()) {
            return new InvoiceGenerationResult(period, 0, 0, 0, 0,
                    new ArrayList<>(List.of("No active enrollments matched")));
        }

        // Skip enrollments that already have a NON-CANCELLED invoice for this period.
        List<Long> candidateIds = candidates.stream().map(Enrollment::getId).toList();
        Set<Long> alreadyInvoiced = new HashSet<>(
                invoiceRepo.findEnrollmentsAlreadyInvoiced(period, candidateIds));

        // For async progress reporting: total = total candidates we'll iterate.
        if (taskId != null) progressTracker.setTotal(taskId, candidates.size());

        // Preload pricing once.
        Map<Long, Map<Integer, BigDecimal>> pricingByCategoryThenClass = new HashMap<>();
        for (FeeCategory c : activeCategories) {
            Map<Integer, BigDecimal> byClass = new HashMap<>();
            for (FeePricing p : pricingRepo.findByFeeCategoryId(c.getId())) {
                if (Boolean.TRUE.equals(p.getIsActive())) {
                    byClass.put(p.getClassId(), p.getAmount());
                }
            }
            pricingByCategoryThenClass.put(c.getId(), byClass);
        }

        InvoiceGenerationResult result = new InvoiceGenerationResult(period, 0, 0, 0, 0, new ArrayList<>());

        for (Enrollment enrollment : candidates) {
            // Wrap each enrollment in try/finally so the progress counter
            // ticks even for already-invoiced (skipped) students.
            if (alreadyInvoiced.contains(enrollment.getId())) {
                result.setSkippedExisting(result.getSkippedExisting() + 1);
                if (taskId != null) progressTracker.increment(taskId);
                continue;
            }

            try {
                // Goes through Spring proxy → REQUIRES_NEW tx for this single invoice.
                CreateInvoiceOutcome outcome = self.createOneInvoiceTx(
                        enrollment, period, arAccountId, invoicePrefix,
                        issuedDate, dueDate, activeCategories,
                        pricingByCategoryThenClass, generatedBy);
                switch (outcome) {
                    case CREATED -> result.setCreatedCount(result.getCreatedCount() + 1);
                    case NO_FEES -> result.setSkippedNoFees(result.getSkippedNoFees() + 1);
                }
            } catch (Exception ex) {
                result.setFailedCount(result.getFailedCount() + 1);
                String studentLabel = enrollment.getStudent() != null
                        ? (enrollment.getStudent().getNameEnglish() != null
                            ? enrollment.getStudent().getNameEnglish()
                            : enrollment.getStudentSystemId())
                        : ("enrollment #" + enrollment.getId());
                String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                result.getWarnings().add("Failed for " + studentLabel + ": " + msg);
            } finally {
                if (taskId != null) progressTracker.increment(taskId);
            }
        }

        return result;
    }

    /**
     * Creates a single invoice + lines + accrual journal entry.
     * Runs in its OWN transaction. Throws on any failure — caller decides
     * whether to abort the batch or just record a warning.
     *
     * MUST be public so the Spring proxy can intercept the call.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CreateInvoiceOutcome createOneInvoiceTx(
            Enrollment enrollment,
            LocalDate period,
            Long arAccountId,
            String invoicePrefix,
            LocalDate issuedDate,
            LocalDate dueDate,
            List<FeeCategory> activeCategories,
            Map<Long, Map<Integer, BigDecimal>> pricingByCategoryThenClass,
            String generatedBy) {

        // Re-check existence inside the tx — guards against concurrent batches.
        if (invoiceRepo.findEnrollmentsAlreadyInvoiced(period, List.of(enrollment.getId()))
                .contains(enrollment.getId())) {
            return CreateInvoiceOutcome.NO_FEES; // treated as skipped; outer caller already counted skipped
        }

        Integer classId = enrollment.getStudentClass() != null
                ? enrollment.getStudentClass().getId() : null;
        if (classId == null) {
            throw new IllegalStateException("Enrollment has no class assigned");
        }

        // Build line specs.
        record LineSpec(Long feeCategoryId, String name, Long incomeLedgerId, BigDecimal amount) {}
        List<LineSpec> specs = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (FeeCategory cat : activeCategories) {
            BigDecimal amount = pricingByCategoryThenClass.get(cat.getId()).get(classId);
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) continue;
            specs.add(new LineSpec(cat.getId(), cat.getName(), cat.getIncomeLedgerId(), amount));
            total = total.add(amount);
        }
        if (specs.isEmpty()) {
            return CreateInvoiceOutcome.NO_FEES;
        }

        // Auto-append the platform fee (Edunix's per-enrollment cut). Cached
        // hourly so we don't hit platform_admin once per student per month.
        // Lands as a separate line crediting Platform Payable so:
        //   Dr AR           = student total incl. platform fee
        //   Cr Income       = per-fee-category ledgers
        //   Cr Platform Pay = platform fee
        BigDecimal platformFeeRate = platformFeeService.getPerEnrollmentRate();
        AccountingSettings accSettings = settingsService.getRequiredForPosting();
        if (platformFeeRate.compareTo(BigDecimal.ZERO) > 0
                && accSettings.getPlatformPayableAccountId() != null) {
            specs.add(new LineSpec(null,
                    "Platform Fee (Edunix)",
                    accSettings.getPlatformPayableAccountId(),
                    platformFeeRate));
            total = total.add(platformFeeRate);
        }

        // Header.
        Invoice inv = new Invoice();
        inv.setInvoiceNumber(nextInvoiceNumber(invoicePrefix, period));
        inv.setEnrollmentId(enrollment.getId());
        inv.setBillingPeriod(period);
        inv.setIssuedDate(issuedDate);
        inv.setDueDate(dueDate);
        inv.setTotalAmount(total);
        inv.setPaidAmount(BigDecimal.ZERO);
        inv.setStatus(InvoiceStatus.PENDING);
        Invoice savedInv = invoiceRepo.save(inv);

        // Lines.
        for (LineSpec spec : specs) {
            InvoiceLine line = new InvoiceLine();
            line.setInvoiceId(savedInv.getId());
            line.setFeeCategoryId(spec.feeCategoryId());
            line.setFeeCategoryName(spec.name());
            line.setIncomeLedgerId(spec.incomeLedgerId());
            line.setAmount(spec.amount());
            lineRepo.save(line);
        }

        // Accrual journal: Dr AR (total), Cr each unique income ledger (summed).
        JournalEntryRequest jReq = new JournalEntryRequest();
        jReq.setEntryDate(issuedDate);
        jReq.setDescription("Invoice " + savedInv.getInvoiceNumber()
                + " — enrollment " + enrollment.getId()
                + " — " + period.format(DateTimeFormatter.ofPattern("MMM yyyy")));
        jReq.setReferenceType(JournalReferenceType.INVOICE);
        jReq.setReferenceId(savedInv.getId());

        List<JournalLineRequest> jLines = new ArrayList<>();
        JournalLineRequest dr = new JournalLineRequest();
        dr.setAccountId(arAccountId);
        dr.setDebit(total);
        dr.setDescription("Accounts Receivable — " + savedInv.getInvoiceNumber());
        jLines.add(dr);

        Map<Long, BigDecimal> creditsByLedger = new HashMap<>();
        for (LineSpec spec : specs) {
            creditsByLedger.merge(spec.incomeLedgerId(), spec.amount(), BigDecimal::add);
        }
        for (Map.Entry<Long, BigDecimal> e : creditsByLedger.entrySet()) {
            JournalLineRequest cr = new JournalLineRequest();
            cr.setAccountId(e.getKey());
            cr.setCredit(e.getValue());
            cr.setDescription("Fee income — " + savedInv.getInvoiceNumber());
            jLines.add(cr);
        }
        jReq.setLines(jLines);
        JournalEntryResponse jeResp = journalService.post(jReq, generatedBy);

        savedInv.setJournalEntryId(jeResp.getId());
        invoiceRepo.save(savedInv);

        return CreateInvoiceOutcome.CREATED;
    }

    public enum CreateInvoiceOutcome { CREATED, NO_FEES }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String nextInvoiceNumber(String prefix, LocalDate period) {
        String monthTag = period.format(MONTH_TAG);
        String search = prefix + "-" + monthTag + "-";
        List<String> recent = invoiceRepo.findRecentNumbersByPrefix(search, PageRequest.of(0, 1));
        int next = 1;
        if (!recent.isEmpty()) {
            String last = recent.get(0);
            int dash = last.lastIndexOf('-');
            if (dash > 0 && dash < last.length() - 1) {
                try { next = Integer.parseInt(last.substring(dash + 1)) + 1; }
                catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s%05d", search, next);
    }

    private InvoiceResponse hydrate(Invoice inv) {
        List<InvoiceLine> lines = lineRepo.findByInvoiceId(inv.getId());
        List<InvoiceLineResponse> lineDtos = lines.stream().map(InvoiceLineResponse::from).toList();
        String studentSystemId = null, studentName = null, className = null;
        Enrollment e = enrollmentRepo.findById(inv.getEnrollmentId()).orElse(null);
        if (e != null) {
            if (e.getStudent() != null) {
                studentSystemId = e.getStudent().getStudentSystemId();
                studentName = e.getStudent().getNameEnglish();
            }
            if (e.getStudentClass() != null) {
                className = e.getStudentClass().getName();
            }
        }
        return InvoiceResponse.from(inv, studentSystemId, studentName, className, lineDtos);
    }
}
