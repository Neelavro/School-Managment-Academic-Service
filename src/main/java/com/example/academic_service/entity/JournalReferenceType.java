package com.example.academic_service.entity;

/**
 * What kind of business event produced this journal entry.
 * Used for filtering, traceability, and reversal handling.
 */
public enum JournalReferenceType {
    INVOICE,            // Accrual from invoice generation
    PAYMENT_ONLINE,     // SSLCommerz or similar gateway
    PAYMENT_BANK,       // Bank deposit
    PAYMENT_CASH,       // In-person cash collection
    RECEIPT_VOUCHER,    // Manual receipt voucher
    PAYMENT_VOUCHER,    // Manual payment voucher
    CONTRA,             // Cash ↔ Bank transfer voucher
    MANUAL,             // Generic journal voucher
    ADJUSTMENT          // Reversing entry posted to correct a prior entry
}
