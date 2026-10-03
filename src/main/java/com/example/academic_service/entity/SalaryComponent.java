package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** A part of a salary (Basic, House Rent, Medical…). Each posts to its own expense account. */
@Entity
@Table(name = "salary_component")
@Getter
@Setter
public class SalaryComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Leaf EXPENSE account the salary for this part is posted to. */
    @Column(name = "expense_ledger_id", nullable = false)
    private Long expenseLedgerId;

    /** The basic salary: festival bonuses given as a percentage are a percentage of this part. */
    @Column(name = "is_basic", nullable = false)
    private Boolean isBasic;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (isBasic == null) isBasic = false;
        if (isActive == null) isActive = true;
        if (sortOrder == null) sortOrder = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
