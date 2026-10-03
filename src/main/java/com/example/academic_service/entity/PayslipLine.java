package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "payslip_line")
@Getter
@Setter
public class PayslipLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payslip_id", nullable = false)
    private Long payslipId;

    /** The salary part; null for a festival bonus line. */
    @Column(name = "component_id")
    private Long componentId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "expense_ledger_id", nullable = false)
    private Long expenseLedgerId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
