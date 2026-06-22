package com.example.academic_service.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IncomeStatementDto {
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<AccountTotalsDto> incomeRows;
    private List<AccountTotalsDto> expenseRows;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    /** Income − Expense. Positive = profit, negative = loss. */
    private BigDecimal netProfit;
}
