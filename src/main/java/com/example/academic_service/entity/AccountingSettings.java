package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Singleton row (id = 1) that holds school-wide accounting configuration.
 * Holds the foreign-key ids of the special-purpose ledger accounts.
 */
@Entity
@Table(name = "accounting_settings")
@Getter
@Setter
public class AccountingSettings {

    @Id
    private Long id;

    /** Leaf ASSET account — Accounts Receivable from students. */
    @Column(name = "ar_account_id")
    private Long arAccountId;

    /** Leaf ASSET account — Cash in Hand. Used by in-person payments. */
    @Column(name = "cash_account_id")
    private Long cashAccountId;

    /** Leaf ASSET account — Online Payment Gateway Clearing. Receives Dr when
        an SSLCommerz payment validates; cleared later when the gateway settles
        the actual cash into a Bank account. */
    @Column(name = "gateway_clearing_account_id")
    private Long gatewayClearingAccountId;

    /** Leaf EXPENSE account — Software / Platform Fee. Dr side of the platform
        commission journal entry. */
    @Column(name = "platform_fee_account_id")
    private Long platformFeeAccountId;

    /** Leaf LIABILITY account — Payable to Platform. Cr side of the commission
        entry; reduced when the SaaS platform owner settles their invoice. */
    @Column(name = "platform_payable_account_id")
    private Long platformPayableAccountId;

    /** Percent fee per online transaction, e.g. 2.0 for 2%. */
    @Column(name = "platform_fee_percent", precision = 5, scale = 2)
    private java.math.BigDecimal platformFeePercent;

    /** Flat fee in BDT per online transaction, e.g. 5.0. Added on top of percent. */
    @Column(name = "platform_fee_flat", precision = 15, scale = 2)
    private java.math.BigDecimal platformFeeFlat;

    @Column(name = "invoice_due_days", nullable = false)
    private Integer invoiceDueDays;

    @Column(name = "invoice_number_prefix", length = 16, nullable = false)
    private String invoiceNumberPrefix;

    @Column(name = "journal_number_prefix", length = 16, nullable = false)
    private String journalNumberPrefix;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = 1L;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (invoiceDueDays == null) invoiceDueDays = 7;
        if (invoiceNumberPrefix == null) invoiceNumberPrefix = "INV";
        if (journalNumberPrefix == null) journalNumberPrefix = "JE";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
