package com.example.academic_service.entity;

/**
 * Manual voucher categories. Each maps to a specific posting pattern
 * and validation rule set; the underlying double-entry mechanism is the
 * same JournalEntryService used by invoices and payments.
 *
 *   RECEIPT  — Dr Cash/Bank   Cr any non-EXPENSE leaf
 *   PAYMENT  — Dr any non-INCOME leaf  Cr Cash/Bank
 *   CONTRA   — Dr ASSET (cash/bank)    Cr different ASSET (cash/bank)
 *   JOURNAL  — Any balanced multi-line set (2+ lines, all leaves)
 */
public enum VoucherType {
    RECEIPT,
    PAYMENT,
    CONTRA,
    JOURNAL;

    public String defaultPrefix() {
        return switch (this) {
            case RECEIPT -> "RV";
            case PAYMENT -> "PV";
            case CONTRA  -> "CV";
            case JOURNAL -> "JV";
        };
    }

    public JournalReferenceType toJournalReference() {
        return switch (this) {
            case RECEIPT -> JournalReferenceType.RECEIPT_VOUCHER;
            case PAYMENT -> JournalReferenceType.PAYMENT_VOUCHER;
            case CONTRA  -> JournalReferenceType.CONTRA;
            case JOURNAL -> JournalReferenceType.MANUAL;
        };
    }
}
