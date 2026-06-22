package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One line on a JournalEntry. Exactly ONE of debit_amount or credit_amount
 * is non-null and > 0. Enforced in the JournalEntryService before save.
 */
@Entity
@Table(
    name = "journal_entry_lines",
    indexes = {
        @Index(name = "idx_jel_entry", columnList = "journal_entry_id"),
        @Index(name = "idx_jel_account", columnList = "account_id")
    }
)
@Getter
@Setter
public class JournalEntryLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "journal_entry_id", nullable = false)
    private Long journalEntryId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "debit_amount", precision = 15, scale = 2)
    private BigDecimal debitAmount;

    @Column(name = "credit_amount", precision = 15, scale = 2)
    private BigDecimal creditAmount;

    @Column(name = "line_description", length = 500)
    private String lineDescription;
}
