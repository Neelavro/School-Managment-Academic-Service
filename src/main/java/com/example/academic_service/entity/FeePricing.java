package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "fee_pricing",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_fee_pricing_cat_class",
                          columnNames = {"fee_category_id", "class_id"})
    },
    indexes = {
        @Index(name = "idx_fee_pricing_category", columnList = "fee_category_id"),
        @Index(name = "idx_fee_pricing_class", columnList = "class_id")
    }
)
@Getter
@Setter
public class FeePricing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fee_category_id", nullable = false)
    private Long feeCategoryId;

    @Column(name = "class_id", nullable = false)
    private Integer classId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
