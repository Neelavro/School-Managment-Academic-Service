package com.example.academic_service.dto;

import com.example.academic_service.entity.AccountType;
import com.example.academic_service.entity.ChartOfAccount;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChartOfAccountResponse {
    private Long id;
    private String accountCode;
    private String accountName;
    private AccountType accountType;
    private String normalBalance;
    private Long parentId;
    private String parentCode;
    private Boolean isGroup;
    private Boolean isActive;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChartOfAccountResponse from(ChartOfAccount a, String parentCode) {
        ChartOfAccountResponse r = new ChartOfAccountResponse();
        r.id = a.getId();
        r.accountCode = a.getAccountCode();
        r.accountName = a.getAccountName();
        r.accountType = a.getAccountType();
        r.normalBalance = a.getAccountType() != null
                ? a.getAccountType().getNormalBalance().name()
                : null;
        r.parentId = a.getParentId();
        r.parentCode = parentCode;
        r.isGroup = a.getIsGroup();
        r.isActive = a.getIsActive();
        r.description = a.getDescription();
        r.createdAt = a.getCreatedAt();
        r.updatedAt = a.getUpdatedAt();
        return r;
    }
}
