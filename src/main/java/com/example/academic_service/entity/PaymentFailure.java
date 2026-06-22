package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Historical record of a failed or cancelled payment attempt.
 *
 * Rows here exist purely for **support and audit**:
 *   - "Parent says they paid but invoice shows unpaid" → look up tran_id, see why
 *   - SSLCommerz dispute resolution → raw gateway response is preserved
 *   - Detect repeated failures from a problematic BIN / card / parent
 *
 * Failure rows are NOT linked to journal entries and have no financial effect.
 * Status is always FAILED or CANCELLED.
 *
 * Lifecycle: when a payment goes wrong, the {@code payments} row is COPIED
 * into this table (with the failure metadata) and then DELETED from
 * {@code payments}. The move happens inside one transaction.
 */
@Entity
@Table(
    name = "payment_failures",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_payment_failure_tran_id", columnNames = "tran_id")
    },
    indexes = {
        @Index(name = "idx_payment_failure_invoice", columnList = "invoice_id"),
        @Index(name = "idx_payment_failure_status", columnList = "status"),
        @Index(name = "idx_payment_failure_failed_at", columnList = "failed_at")
    }
)
@Getter
@Setter
public class PaymentFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tran_id", nullable = false, length = 64)
    private String tranId;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 30)
    private PaymentMethod method;

    /** Always FAILED or CANCELLED. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "gateway_tran_id", length = 128)
    private String gatewayTranId;

    @Column(name = "gateway_card_type", length = 64)
    private String gatewayCardType;

    @Column(name = "gateway_response", columnDefinition = "LONGTEXT")
    private String gatewayResponse;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

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

    @Column(name = "failed_at", nullable = false)
    private LocalDateTime failedAt;
}
