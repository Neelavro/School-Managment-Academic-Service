package com.example.academic_service.service;

import com.example.academic_service.dto.reports.*;
import com.example.academic_service.entity.AccountType;
import com.example.academic_service.entity.ChartOfAccount;
import com.example.academic_service.entity.JournalEntry;
import com.example.academic_service.entity.JournalEntryLine;
import com.example.academic_service.repository.ChartOfAccountRepository;
import com.example.academic_service.repository.JournalEntryLineRepository;
import com.example.academic_service.repository.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Aggregation engine for the 4 financial reports:
 *   - Ledger Report (one account, transactions + running balance)
 *   - Trial Balance (all accounts, Dr/Cr columns)
 *   - Income Statement (Income − Expense = Net Profit)
 *   - Balance Sheet (Assets = Liabilities + Retained Earnings)
 *
 * All built on top of one foundation: per-account Dr/Cr totals over a date range.
 * Reversed entries are INCLUDED in totals — the reversing entry is itself a
 * separate row that nets the original to zero. The is_reversed flag is purely
 * a UI hint.
 *
 * Group/header accounts never appear in reports — they're organizational only.
 * Postings can only target leaf accounts.
 */
@Service
@RequiredArgsConstructor
public class ReportingService {

    private final JournalEntryLineRepository lineRepo;
    private final JournalEntryRepository entryRepo;
    private final ChartOfAccountRepository coaRepo;

    // ── Ledger Report ─────────────────────────────────────────────────────

    public LedgerReportDto getLedger(Long accountId, LocalDate from, LocalDate to) {
        if (accountId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accountId is required");
        }
        ChartOfAccount account = coaRepo.findById(accountId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        if (Boolean.TRUE.equals(account.getIsGroup())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot generate a ledger for a group account");
        }

        // Opening balance — everything BEFORE the from-date.
        LocalDate openingTo = from != null ? from.minusDays(1) : null;
        BigDecimal openingBalance = computeNetOnNormalSide(account, openingTo);

        // Lines within range.
        List<Object[]> rows = lineRepo.findLedgerLines(accountId, from, to);
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal running = openingBalance;

        List<LedgerReportDto.LedgerLineDto> lines = new ArrayList<>();
        for (Object[] row : rows) {
            JournalEntryLine line = (JournalEntryLine) row[0];
            JournalEntry entry = (JournalEntry) row[1];

            BigDecimal d = line.getDebitAmount() != null ? line.getDebitAmount() : BigDecimal.ZERO;
            BigDecimal c = line.getCreditAmount() != null ? line.getCreditAmount() : BigDecimal.ZERO;
            totalDebit = totalDebit.add(d);
            totalCredit = totalCredit.add(c);

            // Move the running balance on the normal-balance side.
            BigDecimal delta = isDebitNormal(account.getAccountType())
                    ? d.subtract(c)
                    : c.subtract(d);
            running = running.add(delta);

            lines.add(new LedgerReportDto.LedgerLineDto(
                    entry.getId(),
                    entry.getEntryNumber(),
                    entry.getEntryDate(),
                    entry.getDescription(),
                    line.getLineDescription(),
                    entry.getReferenceType() != null ? entry.getReferenceType().name() : null,
                    entry.getReferenceId(),
                    d.compareTo(BigDecimal.ZERO) > 0 ? d : null,
                    c.compareTo(BigDecimal.ZERO) > 0 ? c : null,
                    running,
                    entry.getIsReversed()
            ));
        }

        return new LedgerReportDto(
                account.getId(), account.getAccountCode(), account.getAccountName(),
                account.getAccountType(), account.getAccountType().getNormalBalance().name(),
                from, to,
                openingBalance, running, totalDebit, totalCredit,
                lines);
    }

    // ── Trial Balance ─────────────────────────────────────────────────────

    public TrialBalanceDto getTrialBalance(LocalDate asOf) {
        Map<Long, BigDecimal[]> totals = aggregateBy(null, asOf);
        Map<Long, ChartOfAccount> accountsById = loadAccountsById(totals.keySet());

        List<AccountTotalsDto> rows = new ArrayList<>();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (var e : totals.entrySet()) {
            ChartOfAccount a = accountsById.get(e.getKey());
            if (a == null || Boolean.TRUE.equals(a.getIsGroup())) continue; // skip ghosts/groups
            BigDecimal[] dr_cr = e.getValue();
            BigDecimal d = dr_cr[0], c = dr_cr[1];
            if (d.compareTo(BigDecimal.ZERO) == 0 && c.compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal net = isDebitNormal(a.getAccountType()) ? d.subtract(c) : c.subtract(d);
            rows.add(new AccountTotalsDto(a.getId(), a.getAccountCode(), a.getAccountName(),
                    a.getAccountType(), d, c, net));
            totalDebit = totalDebit.add(d);
            totalCredit = totalCredit.add(c);
        }
        rows.sort(Comparator.comparing(AccountTotalsDto::getAccountCode));

        return new TrialBalanceDto(
                asOf != null ? asOf : LocalDate.now(),
                rows, totalDebit, totalCredit,
                totalDebit.compareTo(totalCredit) == 0);
    }

    // ── Income Statement ──────────────────────────────────────────────────

    public IncomeStatementDto getIncomeStatement(LocalDate from, LocalDate to) {
        Map<Long, BigDecimal[]> totals = aggregateBy(from, to);
        Map<Long, ChartOfAccount> accountsById = loadAccountsById(totals.keySet());

        List<AccountTotalsDto> incomeRows = new ArrayList<>();
        List<AccountTotalsDto> expenseRows = new ArrayList<>();
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (var e : totals.entrySet()) {
            ChartOfAccount a = accountsById.get(e.getKey());
            if (a == null || Boolean.TRUE.equals(a.getIsGroup())) continue;
            BigDecimal d = e.getValue()[0], c = e.getValue()[1];

            switch (a.getAccountType()) {
                case INCOME -> {
                    BigDecimal net = c.subtract(d);  // income increases with credits
                    if (net.compareTo(BigDecimal.ZERO) == 0) break;
                    incomeRows.add(new AccountTotalsDto(a.getId(), a.getAccountCode(),
                            a.getAccountName(), a.getAccountType(), d, c, net));
                    totalIncome = totalIncome.add(net);
                }
                case EXPENSE -> {
                    BigDecimal net = d.subtract(c);  // expense increases with debits
                    if (net.compareTo(BigDecimal.ZERO) == 0) break;
                    expenseRows.add(new AccountTotalsDto(a.getId(), a.getAccountCode(),
                            a.getAccountName(), a.getAccountType(), d, c, net));
                    totalExpense = totalExpense.add(net);
                }
                default -> { /* skip ASSET/LIABILITY for P&L */ }
            }
        }
        incomeRows.sort(Comparator.comparing(AccountTotalsDto::getAccountCode));
        expenseRows.sort(Comparator.comparing(AccountTotalsDto::getAccountCode));

        return new IncomeStatementDto(from, to, incomeRows, expenseRows,
                totalIncome, totalExpense, totalIncome.subtract(totalExpense));
    }

    // ── Balance Sheet ─────────────────────────────────────────────────────

    public BalanceSheetDto getBalanceSheet(LocalDate asOf) {
        LocalDate cutoff = asOf != null ? asOf : LocalDate.now();
        Map<Long, BigDecimal[]> totals = aggregateBy(null, cutoff);
        Map<Long, ChartOfAccount> accountsById = loadAccountsById(totals.keySet());

        List<AccountTotalsDto> assetRows = new ArrayList<>();
        List<AccountTotalsDto> liabilityRows = new ArrayList<>();
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (var e : totals.entrySet()) {
            ChartOfAccount a = accountsById.get(e.getKey());
            if (a == null || Boolean.TRUE.equals(a.getIsGroup())) continue;
            BigDecimal d = e.getValue()[0], c = e.getValue()[1];

            switch (a.getAccountType()) {
                case ASSET -> {
                    BigDecimal net = d.subtract(c);
                    if (net.compareTo(BigDecimal.ZERO) == 0) break;
                    assetRows.add(new AccountTotalsDto(a.getId(), a.getAccountCode(),
                            a.getAccountName(), a.getAccountType(), d, c, net));
                    totalAssets = totalAssets.add(net);
                }
                case LIABILITY -> {
                    BigDecimal net = c.subtract(d);
                    if (net.compareTo(BigDecimal.ZERO) == 0) break;
                    liabilityRows.add(new AccountTotalsDto(a.getId(), a.getAccountCode(),
                            a.getAccountName(), a.getAccountType(), d, c, net));
                    totalLiabilities = totalLiabilities.add(net);
                }
                case INCOME -> totalIncome = totalIncome.add(c.subtract(d));
                case EXPENSE -> totalExpense = totalExpense.add(d.subtract(c));
            }
        }
        assetRows.sort(Comparator.comparing(AccountTotalsDto::getAccountCode));
        liabilityRows.sort(Comparator.comparing(AccountTotalsDto::getAccountCode));

        BigDecimal retainedEarnings = totalIncome.subtract(totalExpense);
        BigDecimal rhs = totalLiabilities.add(retainedEarnings);

        return new BalanceSheetDto(cutoff, assetRows, liabilityRows,
                totalAssets, totalLiabilities, retainedEarnings, rhs,
                totalAssets.setScale(2, RoundingMode.HALF_UP)
                        .compareTo(rhs.setScale(2, RoundingMode.HALF_UP)) == 0);
    }

    // ── Internals ─────────────────────────────────────────────────────────

    private Map<Long, BigDecimal[]> aggregateBy(LocalDate from, LocalDate to) {
        List<Object[]> rows = lineRepo.aggregateByAccount(from, to);
        Map<Long, BigDecimal[]> out = new HashMap<>();
        for (Object[] row : rows) {
            Long accountId = ((Number) row[0]).longValue();
            BigDecimal d = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            BigDecimal c = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            out.put(accountId, new BigDecimal[]{d, c});
        }
        return out;
    }

    private Map<Long, ChartOfAccount> loadAccountsById(Collection<Long> ids) {
        if (ids.isEmpty()) return Collections.emptyMap();
        Map<Long, ChartOfAccount> out = new HashMap<>();
        for (ChartOfAccount a : coaRepo.findAllById(ids)) out.put(a.getId(), a);
        return out;
    }

    /**
     * For a SINGLE account, compute the net balance on its normal side
     * up to and including the given date.
     */
    private BigDecimal computeNetOnNormalSide(ChartOfAccount account, LocalDate to) {
        List<Object[]> rows = lineRepo.aggregateByAccount(null, to);
        for (Object[] row : rows) {
            Long aid = ((Number) row[0]).longValue();
            if (!aid.equals(account.getId())) continue;
            BigDecimal d = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            BigDecimal c = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            return isDebitNormal(account.getAccountType()) ? d.subtract(c) : c.subtract(d);
        }
        return BigDecimal.ZERO;
    }

    private static boolean isDebitNormal(AccountType type) {
        return type == AccountType.ASSET || type == AccountType.EXPENSE;
    }
}
