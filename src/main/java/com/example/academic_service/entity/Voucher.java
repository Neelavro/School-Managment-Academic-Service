package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * User-facing manual voucher document. Wraps an immutable JournalEntry that
 * actually records the double-entry posting. The voucher itself is also
 * immutable post-creation; corrections happen via reversal entries.
 */
@Entity
@Table(
    name = "vouchers",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_voucher_number", columnNames = "voucher_number")
    },
    indexes = {
        @Index(name = "idx_voucher_date", columnList = "voucher_date"),
        @Index(name = "idx_voucher_type", columnList = "voucher_type"),
        @Index(name = "idx_voucher_journal", columnList = "journal_entry_id"),
        @Index(name = "idx_voucher_reversed", columnList = "is_reversed")
    }
)
@Getter
@Setter
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voucher_number", nullable = false, length = 64)
    private String voucherNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "voucher_type", nullable = false, length = 20)
    private VoucherType voucherType;

    @Column(name = "voucher_date", nullable = false)
    private LocalDate voucherDate;

    @Column(name = "narration", columnDefinition = "TEXT")
    private String narration;

    /** Summed Dr amount of the underlying journal entry — for at-a-glance display. */
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    /** Optional payee/payer free text — "Bangladesh Power Development Board", "Mr. Karim", etc. */
    @Column(name = "party_name", length = 255)
    private String partyName;

    @Column(name = "journal_entry_id", nullable = false)
    private Long journalEntryId;

    @Column(name = "is_reversed", nullable = false)
    private Boolean isReversed;

    @Column(name = "reverses_voucher_id")
    private Long reversesVoucherId;

    @Column(name = "reversed_by_voucher_id")
    private Long reversedByVoucherId;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isReversed == null) isReversed = false;
    }
}
