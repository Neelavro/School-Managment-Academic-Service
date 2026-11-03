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
 *   - EQUITY    : CREDIT  (funds and reserves, e.g. General Fund, Building Fund; user, 2026-11-03)
 *
 * The year's net income (Income - Expense) is not an account: the balance sheet shows it as retained earnings.
 */
public enum AccountType {
    ASSET,
    LIABILITY,
    INCOME,
    EXPENSE,
    EQUITY;

    public NormalBalance getNormalBalance() {
        return switch (this) {
            case ASSET, EXPENSE -> NormalBalance.DEBIT;
            case LIABILITY, INCOME, EQUITY -> NormalBalance.CREDIT;
        };
    }

    public enum NormalBalance { DEBIT, CREDIT }
}
