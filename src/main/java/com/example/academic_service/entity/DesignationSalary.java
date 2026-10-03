package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** The standard amount of one salary part for everyone with a designation. */
@Entity
@Table(name = "designation_salary",
       uniqueConstraints = @UniqueConstraint(name = "uq_designation_salary", columnNames = {"designation_id", "component_id"}))
@Getter
@Setter
public class DesignationSalary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "designation_id", nullable = false)
    private Integer designationId;

    @Column(name = "component_id", nullable = false)
    private Long componentId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
}
