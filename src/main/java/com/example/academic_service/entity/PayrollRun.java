package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One month's salary payroll, or one festival bonus. */
@Entity
@Table(name = "payroll_run")
@Getter
@Setter
public class PayrollRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "run_type", nullable = false, length = 10)
    private PayrollRunType runType;

    /** First day of the month. */
    @Column(name = "period", nullable = false)
    private LocalDate period;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayrollRunStatus status;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "finalised_by", length = 100)
    private String finalisedBy;

    @Column(name = "finalised_at")
    private LocalDateTime finalisedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (status == null) status = PayrollRunStatus.DRAFT;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
