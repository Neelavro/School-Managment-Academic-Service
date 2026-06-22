package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Successful or in-progress payment. Status is always INITIATED or SUCCESS.
 *
 * If a payment fails or is cancelled at the gateway, the row is MOVED to the
 * {@code payment_failures} table (see {@link PaymentFailure}). Keeps this
 * table lean and fast for the common case (parent paid, journal posted).
 *
 * On SUCCESS we link to:
 *   - mainJournalEntryId   (Dr Gateway Clearing / Cr AR)
 *   - feeJournalEntryId    (Dr Platform Fee / Cr Payable to Platform)
 */
@Entity
@Table(
    name = "payments",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_payment_tran_id", columnNames = "tran_id")
    },
    indexes = {
        @Index(name = "idx_payment_invoice", columnList = "invoice_id"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_method", columnList = "method"),
        @Index(name = "idx_payment_initiated_at", columnList = "initiated_at")
    }
)
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Our own transaction id sent to the gateway. Unique. */
    @Column(name = "tran_id", nullable = false, length = 64)
    private String tranId;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 30)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "platform_fee_amount", precision = 15, scale = 2)
    private BigDecimal platformFeeAmount;

    /** Gateway-side transaction id returned on validation (val_id for SSLCommerz). */
    @Column(name = "gateway_tran_id", length = 128)
    private String gatewayTranId;

    /** e.g. "VISA-Dutch Bangla", "BKASH". Diagnostic only. */
    @Column(name = "gateway_card_type", length = 64)
    private String gatewayCardType;

    /** Full raw response JSON from the gateway for audit. */
    @Column(name = "gateway_response", columnDefinition = "LONGTEXT")
    private String gatewayResponse;

    @Column(name = "main_journal_entry_id")
    private Long mainJournalEntryId;

    @Column(name = "fee_journal_entry_id")
    private Long feeJournalEntryId;

    @Column(name = "payer_name", length = 255)
    private String payerName;

    @Column(name = "payer_email", length = 255)
    private String payerEmail;

    @Column(name = "payer_phone", length = 50)
    private String payerPhone;

    @Column(name = "initiated_by", length = 100)
    private String initiatedBy;

    @Column(name = "initiated_ip", length = 64)
    private String initiatedIp;

    @Column(name = "initiated_at", nullable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (initiatedAt == null) initiatedAt = LocalDateTime.now();
    }
}
