package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Immutable header for a balanced double-entry journal posting.
 * Sum of debit lines MUST equal sum of credit lines for every entry.
 * Once posted, entries cannot be edited or deleted. Corrections are
 * applied by posting a reversing entry of type ADJUSTMENT, linked
 * via reverses_entry_id.
 */
@Entity
@Table(
    name = "journal_entries",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_journal_entry_number", columnNames = "entry_number")
    },
    indexes = {
        @Index(name = "idx_journal_entry_date", columnList = "entry_date"),
        @Index(name = "idx_journal_reference", columnList = "reference_type, reference_id"),
        @Index(name = "idx_journal_reversed", columnList = "is_reversed")
    }
)
@Getter
@Setter
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_number", nullable = false, length = 64)
    private String entryNumber;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 30)
    private JournalReferenceType referenceType;

    /** Id of the related business entity (invoice, voucher, payment, etc.). May be null. */
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "total_debit", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDebit;

    @Column(name = "total_credit", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCredit;

    /** True when a reversing entry has been posted against this one. */
    @Column(name = "is_reversed", nullable = false)
    private Boolean isReversed;

    /** Set on the reversing entry: id of the original entry this one cancels. */
    @Column(name = "reverses_entry_id")
    private Long reversesEntryId;

    /** Set on the original entry: id of the reversal entry that cancelled it. */
    @Column(name = "reversed_by_entry_id")
    private Long reversedByEntryId;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isReversed == null) isReversed = false;
    }
    // INTENTIONALLY no @PreUpdate — entries are immutable after posting.
    // The only mutation allowed is flipping is_reversed / reversed_by_entry_id
    // via the reversal flow, which is done by the service layer through repo.save.
}
