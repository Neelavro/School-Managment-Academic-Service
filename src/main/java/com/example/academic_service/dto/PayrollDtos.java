package com.example.academic_service.dto;

import com.example.academic_service.entity.BonusType;
import com.example.academic_service.entity.PayrollRunStatus;
import com.example.academic_service.entity.PayrollRunType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Request and response shapes for payroll (salary setup, staff salaries, payroll runs). */
public final class PayrollDtos {

    private PayrollDtos() {}

    // ── Setup (Accounts) ───────────────────────────────────────────────────

    public record ComponentRequest(String code, String name, Long expenseLedgerId, Boolean isBasic,
                                   Integer sortOrder, Boolean isActive) {}

    public record ComponentResponse(Long id, String code, String name, Long expenseLedgerId,
                                    String expenseLedgerCode, String expenseLedgerName,
                                    boolean isBasic, int sortOrder, boolean isActive) {}

    public record BonusRuleDto(BonusType bonusType, BigDecimal value) {}

    /** One designation's standard salary: componentId → amount. */
    public record DesignationPay(Integer designationId, String designationName, boolean isActive,
                                 Map<Long, BigDecimal> amounts, BigDecimal total, BonusRuleDto bonus,
                                 long staffCount) {}

    /** amounts: componentId → amount (null or 0 = not part of this designation's salary). */
    public record DesignationPayRequest(Map<Long, BigDecimal> amounts, BonusRuleDto bonus) {}

    public record SettingsDto(Long salaryPayableAccountId, Long bonusExpenseAccountId,
                              Long cashAccountId, Long bankAccountId) {}

    public record SettingsResponse(Long salaryPayableAccountId, String salaryPayableAccountName,
                                   Long bonusExpenseAccountId, String bonusExpenseAccountName,
                                   Long cashAccountId, String cashAccountName,
                                   Long bankAccountId, String bankAccountName) {}

    // ── Staff salaries (HR) ────────────────────────────────────────────────

    public record StaffPayRow(Long staffId, String staffSystemId, String name, Integer designationId,
                              String designationName, String employeeType, boolean isActive,
                              BigDecimal total, boolean customised, BonusRuleDto bonus, String bonusSource) {}

    /**
     * One salary part for one employee in a month: the designation's amount, the employee's own
     * amount (null = follows the designation) and since when that applies.
     */
    public record StaffComponentPay(Long componentId, String componentName, boolean isBasic,
                                    BigDecimal designationAmount, BigDecimal staffAmount,
                                    LocalDate staffAmountFrom, BigDecimal amount) {}

    public record StaffSalaryChange(LocalDate effectiveFrom, Long componentId, String componentName,
                                    BigDecimal amount, String createdBy, LocalDateTime createdAt) {}

    public record StaffPayDetail(Long staffId, String staffSystemId, String name, Integer designationId,
                                 String designationName, LocalDate month, List<StaffComponentPay> components,
                                 BigDecimal total, List<StaffSalaryChange> history,
                                 BonusRuleDto designationBonus, BonusRuleDto staffBonus) {}

    /**
     * From effectiveFrom (any day; the month is used) the employee's own amounts.
     * amounts: componentId → amount, or null = follow the designation from that month.
     * Parts left out keep what they had.
     */
    public record StaffSalaryRequest(LocalDate effectiveFrom, Map<Long, BigDecimal> amounts) {}

    // ── Payroll runs (Accounts) ────────────────────────────────────────────

    public record RunCreateRequest(PayrollRunType runType, LocalDate period, String title) {}

    public record RunSummary(Long id, PayrollRunType runType, LocalDate period, String title,
                             PayrollRunStatus status, int staffCount, BigDecimal totalAmount,
                             int paidCount, BigDecimal paidAmount, Long journalEntryId,
                             String createdBy, LocalDateTime createdAt, String finalisedBy,
                             LocalDateTime finalisedAt) {}

    public record LineDto(Long id, Long componentId, String name, BigDecimal amount) {}

    public record PayslipDto(Long id, Long payrollRunId, Long staffId, String staffSystemId, String staffName,
                             String designationName, BigDecimal totalAmount, boolean isPaid, LocalDate paidOn,
                             String paymentMethod, Long paymentJournalEntryId, String paidBy,
                             String phone, String bankName, String bankAccountNumber, List<LineDto> lines) {}

    public record RunDetail(RunSummary run, List<PayslipDto> payslips, List<String> warnings) {}

    /** Draft only: the payslip's lines (componentId null = the bonus line). */
    public record LineAmount(Long componentId, BigDecimal amount) {}

    public record PayslipLinesRequest(List<LineAmount> lines) {}

    /** method CASH or BANK; paidOn defaults to today. */
    public record PayRequest(List<Long> payslipIds, String method, LocalDate paidOn) {}

    /** One employee's payslips across runs (for HR and My Account). */
    public record StaffPayslip(RunSummary run, PayslipDto payslip) {}
}
