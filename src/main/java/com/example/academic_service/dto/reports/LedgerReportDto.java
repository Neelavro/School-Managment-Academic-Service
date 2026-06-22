package com.example.academic_service.dto.reports;

import com.example.academic_service.entity.AccountType;
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
public class LedgerReportDto {
    private Long accountId;
    private String accountCode;
    private String accountName;
    private AccountType accountType;
    private String normalBalance;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private List<LedgerLineDto> lines;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LedgerLineDto {
        private Long journalEntryId;
        private String journalEntryNumber;
        private LocalDate entryDate;
        private String description;
        private String lineDescription;
        private String referenceType;
        private Long referenceId;
        private BigDecimal debit;
        private BigDecimal credit;
        /** Running balance after this line, signed on the account's normal balance side. */
        private BigDecimal runningBalance;
        private Boolean isReversed;
    }
}
