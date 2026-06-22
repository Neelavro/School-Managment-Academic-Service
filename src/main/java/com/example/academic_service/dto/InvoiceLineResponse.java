package com.example.academic_service.dto;

import com.example.academic_service.entity.InvoiceLine;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InvoiceLineResponse {
    private Long id;
    private Long feeCategoryId;
    private String feeCategoryName;
    private BigDecimal amount;

    public static InvoiceLineResponse from(InvoiceLine l) {
        InvoiceLineResponse r = new InvoiceLineResponse();
        r.id = l.getId();
        r.feeCategoryId = l.getFeeCategoryId();
        r.feeCategoryName = l.getFeeCategoryName();
        r.amount = l.getAmount();
        return r;
    }
}
