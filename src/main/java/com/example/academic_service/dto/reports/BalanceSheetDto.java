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
public class BalanceSheetDto {
    private LocalDate asOf;
    private List<AccountTotalsDto> assetRows;
    private List<AccountTotalsDto> liabilityRows;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    /**
     * Cumulative Income − Expense up to {@code asOf}. Acts as the "Equity"
     * substitute since we don't track explicit equity accounts.
     */
    private BigDecimal retainedEarnings;
    /** totalLiabilities + retainedEarnings — should equal totalAssets. */
    private BigDecimal totalEquityAndLiabilities;
    private boolean balanced;
}
