package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Festival bonus for a designation (designationId set) or one employee (staffId set).
 * The employee's own rule wins over the designation's.
 */
@Entity
@Table(name = "bonus_rule")
@Getter
@Setter
public class BonusRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "designation_id", unique = true)
    private Integer designationId;

    @Column(name = "staff_id", unique = true)
    private Long staffId;

    @Enumerated(EnumType.STRING)
    @Column(name = "bonus_type", nullable = false, length = 20)
    private BonusType bonusType;

    @Column(name = "bonus_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal value;
}
