package com.example.academic_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * In-person cash payment received at the school's accounts desk.
 * Posts immediately as SUCCESS — no gateway round-trip.
 */
@Getter
@Setter
public class CashPaymentRequest {

    @NotNull
    private Long invoiceId;

    @NotNull
    @Positive(message = "amount must be > 0")
    private BigDecimal amount;

    /** Free-text name of who paid — optional, falls back to student name. */
    private String payerName;

    /** Optional receipt note shown on the journal entry description. */
    private String narration;
}
