package com.example.academic_service.entity;

/**
 * Account types for the Chart of Accounts.
 *
 * Normal balance is the side on which the account naturally increases:
 *   - ASSET     : DEBIT   (e.g. Cash, Bank, Accounts Receivable)
 *   - LIABILITY : CREDIT  (e.g. Loans, Payable to Platform)
 *   - INCOME    : CREDIT  (e.g. Tuition Fee)
 *   - EXPENSE   : DEBIT   (e.g. Salaries, Platform Fee)
 *
 * Equity is intentionally omitted; net income flows through Income - Expense.
 */
public enum AccountType {
    ASSET,
    LIABILITY,
    INCOME,
    EXPENSE;

    public NormalBalance getNormalBalance() {
        return switch (this) {
            case ASSET, EXPENSE -> NormalBalance.DEBIT;
            case LIABILITY, INCOME -> NormalBalance.CREDIT;
        };
    }

    public enum NormalBalance { DEBIT, CREDIT }
}
