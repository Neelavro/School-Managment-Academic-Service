package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
    name = "invoice_lines",
    indexes = {
        @Index(name = "idx_invoice_line_invoice", columnList = "invoice_id"),
        @Index(name = "idx_invoice_line_category", columnList = "fee_category_id")
    }
)
@Getter
@Setter
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    /** Nullable: system-injected lines (e.g. Platform Fee) have no fee category. */
    @Column(name = "fee_category_id")
    private Long feeCategoryId;

    /** Denormalized for display/reporting even if the category is renamed later. */
    @Column(name = "fee_category_name", nullable = false, length = 255)
    private String feeCategoryName;

    /** Denormalized income ledger so journal posting / reports stay stable. */
    @Column(name = "income_ledger_id", nullable = false)
    private Long incomeLedgerId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
}
