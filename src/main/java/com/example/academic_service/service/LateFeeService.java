package com.example.academic_service.service;

import com.example.academic_service.dto.JournalEntryRequest;
import com.example.academic_service.dto.JournalEntryResponse;
import com.example.academic_service.dto.JournalLineRequest;
import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Late fee: one fee category flagged is_late_fee, priced per class like any fee.
 * Once a monthly fee's due date has passed and it is still not fully paid, the
 * class's late fee is added to it once — as an invoice line, with its own
 * accrual entry (Dr AR / Cr the late fee's income ledger).
 *
 * Runs hourly from {@link com.example.academic_service.scheduler.LateFeeScheduler}
 * and again right before a payment is taken, so the amount collected includes it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LateFeeService {

    public static final ZoneId ZONE = ZoneId.of("Asia/Dhaka");

    private final InvoiceRepository invoiceRepo;
    private final InvoiceLineRepository lineRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final FeeCategoryRepository categoryRepo;
    private final FeePricingRepository pricingRepo;
    private final AccountingSettingsService settingsService;
    private final JournalEntryService journalService;

    @Autowired
    @Lazy
    private LateFeeService self;

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /** The active late fee category, if the school has set one up. */
    public Optional<FeeCategory> activeLateFee() {
        return categoryRepo.findByIsActive(true).stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsLateFee()))
                .findFirst();
    }

    /** classId → late fee amount (only classes with an amount above zero). */
    public Map<Integer, BigDecimal> amountsByClass() {
        Map<Integer, BigDecimal> out = new HashMap<>();
        activeLateFee().ifPresent(cat -> {
            for (FeePricing p : pricingRepo.findByFeeCategoryId(cat.getId())) {
                if (Boolean.TRUE.equals(p.getIsActive()) && p.getAmount() != null
                        && p.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    out.put(p.getClassId(), p.getAmount());
                }
            }
        });
        return out;
    }

    /** Adds the late fee to every overdue, unpaid monthly fee that doesn't have it yet. */
    public int applyAllOverdue() {
        if (activeLateFee().isEmpty()) return 0;
        int added = 0;
        for (Long id : invoiceRepo.findLateFeeCandidates(today())) {
            try {
                if (self.applyIfDue(id, "system:late-fee")) added++;
            } catch (Exception ex) {
                log.warn("Late fee not added to invoice {}: {}", id, ex.getMessage());
            }
        }
        return added;
    }

    /** One invoice in its own transaction. True when a late fee was added. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean applyIfDue(Long invoiceId, String by) {
        return invoiceRepo.findById(invoiceId).map(inv -> applyIfDue(inv, by)).orElse(false);
    }

    /**
     * Joins the caller's transaction (used by the payment paths before they
     * read the outstanding amount). Saves the invoice when it changes.
     */
    @Transactional
    public boolean applyIfDue(Invoice inv, String by) {
        if (inv.getLateFeeAmount() != null) return false;
        if (inv.getStatus() != InvoiceStatus.PENDING && inv.getStatus() != InvoiceStatus.PARTIAL
                && inv.getStatus() != InvoiceStatus.OVERDUE) return false;
        if (inv.getDueDate() == null || !inv.getDueDate().isBefore(today())) return false;
        if (inv.getTotalAmount().compareTo(inv.getPaidAmount()) <= 0) return false;

        FeeCategory cat = activeLateFee().orElse(null);
        if (cat == null) return false;
        Enrollment e = enrollmentRepo.findById(inv.getEnrollmentId()).orElse(null);
        if (e == null || e.getStudentClass() == null) return false;
        BigDecimal amount = pricingRepo.findByFeeCategoryIdAndClassId(cat.getId(), e.getStudentClass().getId())
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(FeePricing::getAmount)
                .orElse(null);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;

        AccountingSettings settings = settingsService.getRequiredForPosting();

        InvoiceLine line = new InvoiceLine();
        line.setInvoiceId(inv.getId());
        line.setFeeCategoryId(cat.getId());
        line.setFeeCategoryName(cat.getName());
        line.setIncomeLedgerId(cat.getIncomeLedgerId());
        line.setAmount(amount);
        lineRepo.save(line);

        JournalEntryRequest j = new JournalEntryRequest();
        j.setEntryDate(today());
        j.setReferenceType(JournalReferenceType.INVOICE);
        j.setReferenceId(inv.getId());
        j.setDescription("Late fee — " + inv.getInvoiceNumber() + " — "
                + inv.getBillingPeriod().format(DateTimeFormatter.ofPattern("MMM yyyy")));
        JournalLineRequest dr = new JournalLineRequest();
        dr.setAccountId(settings.getArAccountId());
        dr.setDebit(amount);
        dr.setDescription("Accounts Receivable — " + inv.getInvoiceNumber());
        JournalLineRequest cr = new JournalLineRequest();
        cr.setAccountId(cat.getIncomeLedgerId());
        cr.setCredit(amount);
        cr.setDescription("Late fee income — " + inv.getInvoiceNumber());
        j.setLines(List.of(dr, cr));
        JournalEntryResponse je = journalService.post(j, by);

        inv.setTotalAmount(inv.getTotalAmount().add(amount));
        inv.setLateFeeAmount(amount);
        inv.setLateFeeAppliedAt(LocalDateTime.now(ZONE));
        inv.setLateFeeJournalEntryId(je.getId());
        invoiceRepo.save(inv);
        return true;
    }
}
