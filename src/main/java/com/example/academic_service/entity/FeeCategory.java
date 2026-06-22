package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "fee_categories",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_fee_category_code", columnNames = "code"),
        @UniqueConstraint(name = "uq_fee_category_name", columnNames = "name")
    },
    indexes = {
        @Index(name = "idx_fee_category_ledger", columnList = "income_ledger_id"),
        @Index(name = "idx_fee_category_active", columnList = "is_active")
    }
)
@Getter
@Setter
public class FeeCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * FK to chart_of_accounts.id. Must reference a leaf INCOME account.
     * Stored as plain Long; integrity enforced in the service layer + DB FK.
     */
    @Column(name = "income_ledger_id", nullable = false)
    private Long incomeLedgerId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** True for recurring fees (monthly tuition). False for one-time (admission, exam). */
    @Column(name = "is_recurring", nullable = false)
    private Boolean isRecurring;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (isRecurring == null) isRecurring = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
