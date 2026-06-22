package com.example.academic_service.dto.reports;

import com.example.academic_service.entity.AccountType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountTotalsDto {
    private Long accountId;
    private String accountCode;
    private String accountName;
    private AccountType accountType;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    /**
     * Signed net for the account's normal balance side:
     *   ASSET / EXPENSE → debit − credit
     *   LIABILITY / INCOME → credit − debit
     */
    private BigDecimal netBalance;
}
