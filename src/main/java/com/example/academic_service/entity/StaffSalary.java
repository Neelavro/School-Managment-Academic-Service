package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One employee's own amount for a salary part, from a month onward (raises keep their history).
 * amount null = from that month the part follows the designation again.
 */
@Entity
@Table(name = "staff_salary",
       uniqueConstraints = @UniqueConstraint(name = "uq_staff_salary", columnNames = {"staff_id", "component_id", "effective_from"}))
@Getter
@Setter
public class StaffSalary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "component_id", nullable = false)
    private Long componentId;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    /** First day of the month the amount starts. */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
