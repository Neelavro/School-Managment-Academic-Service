package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "chart_of_accounts",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_coa_code", columnNames = "account_code")
    },
    indexes = {
        @Index(name = "idx_coa_parent_id", columnList = "parent_id"),
        @Index(name = "idx_coa_account_type", columnList = "account_type"),
        @Index(name = "idx_coa_is_active", columnList = "is_active")
    }
)
@Getter
@Setter
public class ChartOfAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_code", nullable = false, length = 50)
    private String accountCode;

    @Column(name = "account_name", nullable = false, length = 255)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    /**
     * Parent account id. NULL for top-level accounts.
     * Stored as a plain Long (no @ManyToOne) to keep the entity light;
     * traversal happens in the service via repository lookups.
     */
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * True if this account is a group/header (can have children, no transactions).
     * False if it is a leaf/detail account (where journal entries post).
     */
    @Column(name = "is_group", nullable = false)
    private Boolean isGroup;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (isGroup == null) isGroup = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
