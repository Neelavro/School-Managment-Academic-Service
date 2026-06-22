package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "invoices",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_invoice_number", columnNames = "invoice_number")
        // No (enrollment_id, billing_period) unique — cancelled invoices need to
        // coexist with a fresh regeneration. Duplicate prevention is enforced in
        // application logic (InvoiceRepository.findEnrollmentsAlreadyInvoiced
        // filters out cancelled invoices).
    },
    indexes = {
        @Index(name = "idx_invoice_enrollment", columnList = "enrollment_id"),
        @Index(name = "idx_invoice_period", columnList = "billing_period"),
        @Index(name = "idx_invoice_status", columnList = "status"),
        @Index(name = "idx_invoice_due_date", columnList = "due_date"),
        // Speeds up the "is this enrollment already invoiced for this period?" check.
        @Index(name = "idx_invoice_enr_period_status", columnList = "enrollment_id, billing_period, status")
    }
)
@Getter
@Setter
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, length = 64)
    private String invoiceNumber;

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    /** First day of the month the invoice covers. */
    @Column(name = "billing_period", nullable = false)
    private LocalDate billingPeriod;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** The accrual journal entry (Dr AR / Cr Income) created when this invoice was generated. */
    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (paidAmount == null) paidAmount = BigDecimal.ZERO;
        if (status == null) status = InvoiceStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
