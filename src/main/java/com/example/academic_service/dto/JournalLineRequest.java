package com.example.academic_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class JournalLineRequest {
    @NotNull
    private Long accountId;

    /** Exactly one of debit/credit must be set to a positive amount. */
    private BigDecimal debit;
    private BigDecimal credit;

    private String description;
}
