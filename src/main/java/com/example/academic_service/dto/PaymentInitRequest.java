package com.example.academic_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentInitRequest {
    @NotNull
    private Long invoiceId;

    /** Optional payer contact info; falls back to the invoice's enrollment student details. */
    private String payerName;
    private String payerEmail;
    private String payerPhone;
}
