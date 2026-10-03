package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One employee in a payroll run. Name and designation are copied so old payslips stay as they were. */
@Entity
@Table(name = "payslip",
       uniqueConstraints = @UniqueConstraint(name = "uq_payslip_run_staff", columnNames = {"payroll_run_id", "staff_id"}))
@Getter
@Setter
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payroll_run_id", nullable = false)
    private Long payrollRunId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "staff_system_id", length = 50)
    private String staffSystemId;

    @Column(name = "staff_name", nullable = false, length = 255)
    private String staffName;

    @Column(name = "designation_name", length = 255)
    private String designationName;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    /** False until paid; always paid in full. */
    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid;

    @Column(name = "paid_on")
    private LocalDate paidOn;

    /** CASH or BANK. */
    @Column(name = "payment_method", length = 10)
    private String paymentMethod;

    @Column(name = "payment_journal_entry_id")
    private Long paymentJournalEntryId;

    @Column(name = "paid_by", length = 100)
    private String paidBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isPaid == null) isPaid = false;
    }
}
