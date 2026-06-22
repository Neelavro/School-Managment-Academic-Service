package com.example.academic_service.dto;

import com.example.academic_service.entity.FeeCategory;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class FeeCategoryResponse {
    private Long id;
    private String code;
    private String name;
    private Long incomeLedgerId;
    private String incomeLedgerCode;
    private String incomeLedgerName;
    private String description;
    private Boolean isRecurring;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FeeCategoryResponse from(FeeCategory c, String ledgerCode, String ledgerName) {
        FeeCategoryResponse r = new FeeCategoryResponse();
        r.id = c.getId();
        r.code = c.getCode();
        r.name = c.getName();
        r.incomeLedgerId = c.getIncomeLedgerId();
        r.incomeLedgerCode = ledgerCode;
        r.incomeLedgerName = ledgerName;
        r.description = c.getDescription();
        r.isRecurring = c.getIsRecurring();
        r.isActive = c.getIsActive();
        r.createdAt = c.getCreatedAt();
        r.updatedAt = c.getUpdatedAt();
        return r;
    }
}
