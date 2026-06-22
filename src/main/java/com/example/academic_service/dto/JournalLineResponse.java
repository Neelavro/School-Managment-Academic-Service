package com.example.academic_service.dto;

import com.example.academic_service.entity.JournalEntryLine;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class JournalLineResponse {
    private Long id;
    private Long accountId;
    private String accountCode;
    private String accountName;
    private BigDecimal debit;
    private BigDecimal credit;
    private String description;

    public static JournalLineResponse from(JournalEntryLine line, String code, String name) {
        JournalLineResponse r = new JournalLineResponse();
        r.id = line.getId();
        r.accountId = line.getAccountId();
        r.accountCode = code;
        r.accountName = name;
        r.debit = line.getDebitAmount();
        r.credit = line.getCreditAmount();
        r.description = line.getLineDescription();
        return r;
    }
}
