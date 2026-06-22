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
public class TrialBalanceDto {
    private LocalDate asOf;
    private List<AccountTotalsDto> rows;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    /** True iff totalDebit equals totalCredit — books are balanced. */
    private boolean balanced;
}
