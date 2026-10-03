package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Single row (id 1): the accounts payroll posts to. */
@Entity
@Table(name = "payroll_settings")
@Getter
@Setter
public class PayrollSettings {

    @Id
    private Long id;

    /** LIABILITY: salaries owed to staff between finalising and paying. */
    @Column(name = "salary_payable_account_id")
    private Long salaryPayableAccountId;

    /** EXPENSE: festival bonuses. */
    @Column(name = "bonus_expense_account_id")
    private Long bonusExpenseAccountId;

    /** ASSET: paying in cash. */
    @Column(name = "cash_account_id")
    private Long cashAccountId;

    /** ASSET: paying by bank transfer. */
    @Column(name = "bank_account_id")
    private Long bankAccountId;
}
