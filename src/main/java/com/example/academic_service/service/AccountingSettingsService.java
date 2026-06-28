package com.example.academic_service.service;

import com.example.academic_service.dto.AccountingSettingsRequest;
import com.example.academic_service.dto.AccountingSettingsResponse;
import com.example.academic_service.entity.AccountType;
import com.example.academic_service.entity.AccountingSettings;
import com.example.academic_service.entity.ChartOfAccount;
import com.example.academic_service.repository.AccountingSettingsRepository;
import com.example.academic_service.repository.ChartOfAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AccountingSettingsService {

    private static final long SETTINGS_ID = 1L;

    private final AccountingSettingsRepository repo;
    private final ChartOfAccountRepository coaRepo;

    public AccountingSettingsResponse get() {
        return toResponse(ensureRow());
    }

    /**
     * Returns settings with AR configured. Throws if missing — invoice generation
     * needs at minimum the AR account.
     */
    public AccountingSettings getRequiredForPosting() {
        AccountingSettings s = ensureRow();
        if (s.getArAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AR account is not configured. Set it in Accounting Settings first.");
        }
        return s;
    }

    /**
     * Returns settings with the Cash account configured. Throws if missing —
     * in-person payments need both AR and Cash accounts.
     */
    public AccountingSettings getRequiredForCashPayment() {
        AccountingSettings s = getRequiredForPosting();
        if (s.getCashAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cash account is not configured. Set it in Accounting Settings.");
        }
        return s;
    }

    /**
     * Returns settings with online-payment accounts configured. Throws if any
     * are missing — Phase 5 online payments need all three.
     */
    public AccountingSettings getRequiredForOnlinePayment() {
        AccountingSettings s = getRequiredForPosting();
        if (s.getGatewayClearingAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Gateway Clearing account is not configured. Set it in Accounting Settings.");
        }
        // Platform fee is now accrued at invoice generation (Dr AR / Cr
        // Platform Payable). Only Platform Payable is needed; the legacy
        // Platform Fee Expense account is no longer required.
        if (s.getPlatformPayableAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payable to Platform account is not configured. Set it in Accounting Settings.");
        }
        return s;
    }

    @Transactional
    public AccountingSettingsResponse update(AccountingSettingsRequest req) {
        AccountingSettings s = ensureRow();

        applyAccount(req.getArAccountId(), AccountType.ASSET, "AR", s::setArAccountId);
        applyAccount(req.getCashAccountId(), AccountType.ASSET, "Cash", s::setCashAccountId);
        applyAccount(req.getGatewayClearingAccountId(), AccountType.ASSET, "Gateway Clearing",
                s::setGatewayClearingAccountId);
        applyAccount(req.getPlatformFeeAccountId(), AccountType.EXPENSE, "Platform Fee",
                s::setPlatformFeeAccountId);
        applyAccount(req.getPlatformPayableAccountId(), AccountType.LIABILITY, "Payable to Platform",
                s::setPlatformPayableAccountId);

        if (req.getPlatformFeePercent() != null) s.setPlatformFeePercent(req.getPlatformFeePercent());
        if (req.getPlatformFeeFlat() != null) s.setPlatformFeeFlat(req.getPlatformFeeFlat());
        if (req.getInvoiceDueDays() != null) s.setInvoiceDueDays(req.getInvoiceDueDays());
        if (req.getInvoiceNumberPrefix() != null && !req.getInvoiceNumberPrefix().isBlank()) {
            s.setInvoiceNumberPrefix(req.getInvoiceNumberPrefix().trim());
        }
        if (req.getJournalNumberPrefix() != null && !req.getJournalNumberPrefix().isBlank()) {
            s.setJournalNumberPrefix(req.getJournalNumberPrefix().trim());
        }
        repo.save(s);
        return toResponse(s);
    }

    private AccountingSettings ensureRow() {
        return repo.findById(SETTINGS_ID).orElseGet(() -> {
            AccountingSettings s = new AccountingSettings();
            s.setId(SETTINGS_ID);
            return repo.save(s);
        });
    }

    @FunctionalInterface
    private interface LongSetter { void set(Long id); }

    private void applyAccount(Long id, AccountType requiredType, String label, LongSetter setter) {
        if (id == null) { setter.set(null); return; }
        ChartOfAccount a = coaRepo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " account not found"));
        if (a.getAccountType() != requiredType) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    label + " account must be " + requiredType + " (got " + a.getAccountType() + ")");
        }
        if (Boolean.TRUE.equals(a.getIsGroup())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    label + " account must be a leaf, not a group");
        }
        if (Boolean.FALSE.equals(a.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    label + " account is inactive");
        }
        setter.set(id);
    }

    private AccountingSettingsResponse toResponse(AccountingSettings s) {
        ChartOfAccount ar = lookup(s.getArAccountId());
        ChartOfAccount cash = lookup(s.getCashAccountId());
        ChartOfAccount gateway = lookup(s.getGatewayClearingAccountId());
        ChartOfAccount fee = lookup(s.getPlatformFeeAccountId());
        ChartOfAccount payable = lookup(s.getPlatformPayableAccountId());
        return AccountingSettingsResponse.from(s, ar, cash, gateway, fee, payable);
    }

    private ChartOfAccount lookup(Long id) {
        return id != null ? coaRepo.findById(id).orElse(null) : null;
    }
}
