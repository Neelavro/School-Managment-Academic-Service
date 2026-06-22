package com.example.academic_service.entity;

public enum InvoiceStatus {
    PENDING,    // Issued, nothing paid
    PARTIAL,    // Partly paid
    PAID,       // Fully paid
    CANCELLED,  // Cancelled — journal entry reversed
    OVERDUE     // Past due_date and still unpaid/partial (status updated by a job)
}
