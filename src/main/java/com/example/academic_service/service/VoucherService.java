package com.example.academic_service.service;

import com.example.academic_service.dto.*;
import com.example.academic_service.entity.*;
import com.example.academic_service.repository.ChartOfAccountRepository;
import com.example.academic_service.repository.JournalEntryRepository;
import com.example.academic_service.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private static final DateTimeFormatter DATE_TAG = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final VoucherRepository voucherRepo;
    private final JournalEntryRepository journalRepo;
    private final JournalEntryService journalService;
    private final ChartOfAccountRepository coaRepo;

    // ── Reads ──────────────────────────────────────────────────────────────

    public Page<VoucherResponse> search(VoucherType type, LocalDate from, LocalDate to,
                                         int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));
        return voucherRepo.search(type, from, to, pageable)
                .map(v -> VoucherResponse.from(v, lookupJeNumber(v.getJournalEntryId()), null));
    }

    public VoucherResponse getOne(Long id) {
        Voucher v = voucherRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found"));
        JournalEntryResponse je = null;
        try {
            je = journalService.getOne(v.getJournalEntryId());
        } catch (Exception ignored) {
            // tolerate missing journal entry — should never happen but keep the voucher viewable
        }
        return VoucherResponse.from(v, je != null ? je.getEntryNumber() : null, je);
    }

    // ── Posting ────────────────────────────────────────────────────────────

    @Transactional
    public VoucherResponse postReceipt(SimpleVoucherRequest req, String user) {
        validateBasicAmount(req.getAmount());
        ChartOfAccount cashBank = requireLeafActive(req.getCashOrBankAccountId(), "cash/bank");
        ChartOfAccount contra = requireLeafActive(req.getContraAccountId(), "contra");
        if (cashBank.getAccountType() != AccountType.ASSET) {
            throw badType("Receipt: cash/bank account must be ASSET (got " + cashBank.getAccountType() + ")");
        }
        if (contra.getAccountType() == AccountType.EXPENSE) {
            throw badType("Receipt: contra account cannot be EXPENSE");
        }
        if (cashBank.getId().equals(contra.getId())) {
            throw badType("Cash/bank and contra accounts must be different");
        }

        JournalEntryRequest jReq = buildTwoLineRequest(
                req.getVoucherDate(),
                req.getNarration(),
                VoucherType.RECEIPT,
                cashBank.getId(), req.getAmount(), "Received into " + cashBank.getAccountCode(),
                contra.getId(),   req.getAmount(), "From " + contra.getAccountCode()
        );
        return postWithJournal(VoucherType.RECEIPT, req.getVoucherDate(), req.getNarration(),
                req.getPartyName(), req.getAmount(), jReq, user, null);
    }

    @Transactional
    public VoucherResponse postPayment(SimpleVoucherRequest req, String user) {
        validateBasicAmount(req.getAmount());
        ChartOfAccount cashBank = requireLeafActive(req.getCashOrBankAccountId(), "cash/bank");
        ChartOfAccount contra = requireLeafActive(req.getContraAccountId(), "contra");
        if (cashBank.getAccountType() != AccountType.ASSET) {
            throw badType("Payment: cash/bank account must be ASSET (got " + cashBank.getAccountType() + ")");
        }
        if (contra.getAccountType() == AccountType.INCOME) {
            throw badType("Payment: contra account cannot be INCOME");
        }
        if (cashBank.getId().equals(contra.getId())) {
            throw badType("Cash/bank and contra accounts must be different");
        }

        JournalEntryRequest jReq = buildTwoLineRequest(
                req.getVoucherDate(),
                req.getNarration(),
                VoucherType.PAYMENT,
                contra.getId(),   req.getAmount(), "To " + contra.getAccountCode(),
                cashBank.getId(), req.getAmount(), "Paid from " + cashBank.getAccountCode()
        );
        return postWithJournal(VoucherType.PAYMENT, req.getVoucherDate(), req.getNarration(),
                req.getPartyName(), req.getAmount(), jReq, user, null);
    }

    @Transactional
    public VoucherResponse postContra(SimpleVoucherRequest req, String user) {
        validateBasicAmount(req.getAmount());
        ChartOfAccount source = requireLeafActive(req.getCashOrBankAccountId(), "source");
        ChartOfAccount destination = requireLeafActive(req.getContraAccountId(), "destination");
        if (source.getAccountType() != AccountType.ASSET || destination.getAccountType() != AccountType.ASSET) {
            throw badType("Contra: both accounts must be ASSET (cash/bank). Got source="
                    + source.getAccountType() + ", destination=" + destination.getAccountType());
        }
        if (source.getId().equals(destination.getId())) {
            throw badType("Source and destination must be different");
        }

        JournalEntryRequest jReq = buildTwoLineRequest(
                req.getVoucherDate(),
                req.getNarration(),
                VoucherType.CONTRA,
                destination.getId(), req.getAmount(), "To " + destination.getAccountCode(),
                source.getId(),      req.getAmount(), "From " + source.getAccountCode()
        );
        return postWithJournal(VoucherType.CONTRA, req.getVoucherDate(), req.getNarration(),
                req.getPartyName(), req.getAmount(), jReq, user, null);
    }

    @Transactional
    public VoucherResponse postJournal(JournalVoucherRequest req, String user) {
        if (req.getLines() == null || req.getLines().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Journal voucher requires at least two lines");
        }
        // Pre-compute total Dr (= total Cr because the JE engine will reject otherwise).
        BigDecimal total = BigDecimal.ZERO;
        for (JournalLineRequest l : req.getLines()) {
            if (l.getDebit() != null && l.getDebit().compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(l.getDebit());
            }
        }
        validateBasicAmount(total);

        JournalEntryRequest jReq = new JournalEntryRequest();
        jReq.setEntryDate(req.getVoucherDate());
        jReq.setReferenceType(VoucherType.JOURNAL.toJournalReference());
        jReq.setDescription(req.getNarration());
        jReq.setLines(req.getLines());

        return postWithJournal(VoucherType.JOURNAL, req.getVoucherDate(), req.getNarration(),
                req.getPartyName(), total, jReq, user, null);
    }

    // ── Reversal ───────────────────────────────────────────────────────────

    /**
     * Posts a reversing voucher of the SAME type that mirrors the original's journal.
     * Marks the original as reversed and stores the bidirectional link.
     */
    @Transactional
    public VoucherResponse reverse(Long originalId, String reason, String user) {
        Voucher original = voucherRepo.findById(originalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found"));
        if (Boolean.TRUE.equals(original.getIsReversed())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Voucher " + original.getVoucherNumber() + " is already reversed");
        }
        if (original.getReversesVoucherId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot reverse a reversal voucher");
        }

        // Reverse the underlying journal entry; it returns a new entry (ADJUSTMENT type).
        JournalEntryResponse reversalJe = journalService.reverse(
                original.getJournalEntryId(),
                "Voucher " + original.getVoucherNumber() + " reversal"
                        + (reason != null && !reason.isBlank() ? ": " + reason : ""),
                user);

        // Create the reversal voucher header (same type as original).
        Voucher rev = new Voucher();
        rev.setVoucherNumber(nextVoucherNumber(original.getVoucherType(), LocalDate.now()));
        rev.setVoucherType(original.getVoucherType());
        rev.setVoucherDate(LocalDate.now());
        rev.setNarration("Reversal of " + original.getVoucherNumber()
                + (reason != null && !reason.isBlank() ? " — " + reason : ""));
        rev.setTotalAmount(original.getTotalAmount());
        rev.setPartyName(original.getPartyName());
        rev.setJournalEntryId(reversalJe.getId());
        rev.setIsReversed(false);
        rev.setReversesVoucherId(original.getId());
        rev.setCreatedBy(user);
        Voucher savedRev = voucherRepo.save(rev);

        // Mark original.
        original.setIsReversed(true);
        original.setReversedByVoucherId(savedRev.getId());
        voucherRepo.save(original);

        return VoucherResponse.from(savedRev, reversalJe.getEntryNumber(), reversalJe);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private VoucherResponse postWithJournal(VoucherType type, LocalDate date, String narration,
                                             String partyName, BigDecimal totalAmount,
                                             JournalEntryRequest jReq, String user,
                                             Long reversesVoucherId) {
        // Post the underlying journal entry first — its validators check balance + accounts.
        JournalEntryResponse je = journalService.post(jReq, user);

        Voucher v = new Voucher();
        v.setVoucherNumber(nextVoucherNumber(type, date));
        v.setVoucherType(type);
        v.setVoucherDate(date);
        v.setNarration(narration);
        v.setPartyName(partyName);
        v.setTotalAmount(totalAmount);
        v.setJournalEntryId(je.getId());
        v.setIsReversed(false);
        v.setReversesVoucherId(reversesVoucherId);
        v.setCreatedBy(user);
        Voucher saved = voucherRepo.save(v);

        return VoucherResponse.from(saved, je.getEntryNumber(), je);
    }

    private static JournalEntryRequest buildTwoLineRequest(LocalDate date, String narration,
                                                            VoucherType voucherType,
                                                            Long debitAccountId, BigDecimal debitAmount,
                                                            String debitDescription,
                                                            Long creditAccountId, BigDecimal creditAmount,
                                                            String creditDescription) {
        JournalEntryRequest jReq = new JournalEntryRequest();
        jReq.setEntryDate(date);
        jReq.setReferenceType(voucherType.toJournalReference());
        jReq.setDescription(narration);
        List<JournalLineRequest> lines = new ArrayList<>(2);

        JournalLineRequest dr = new JournalLineRequest();
        dr.setAccountId(debitAccountId);
        dr.setDebit(debitAmount);
        dr.setDescription(debitDescription);
        lines.add(dr);

        JournalLineRequest cr = new JournalLineRequest();
        cr.setAccountId(creditAccountId);
        cr.setCredit(creditAmount);
        cr.setDescription(creditDescription);
        lines.add(cr);

        jReq.setLines(lines);
        return jReq;
    }

    private ChartOfAccount requireLeafActive(Long accountId, String label) {
        ChartOfAccount a = coaRepo.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        label + " account not found"));
        if (Boolean.TRUE.equals(a.getIsGroup())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    label + " account must be a leaf, not a group");
        }
        if (Boolean.FALSE.equals(a.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    label + " account is inactive");
        }
        return a;
    }

    private static void validateBasicAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be > 0");
        }
    }

    private static ResponseStatusException badType(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private String nextVoucherNumber(VoucherType type, LocalDate date) {
        String prefix = type.defaultPrefix();
        String dateTag = date.format(DATE_TAG);
        String search = prefix + "-" + dateTag + "-";
        List<String> recent = voucherRepo.findRecentNumbersByPrefix(search, PageRequest.of(0, 1));
        int next = 1;
        if (!recent.isEmpty()) {
            String last = recent.get(0);
            int dash = last.lastIndexOf('-');
            if (dash > 0 && dash < last.length() - 1) {
                try { next = Integer.parseInt(last.substring(dash + 1)) + 1; }
                catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s%04d", search, next);
    }

    private String lookupJeNumber(Long jeId) {
        if (jeId == null) return null;
        return journalRepo.findById(jeId).map(JournalEntry::getEntryNumber).orElse(null);
    }
}
