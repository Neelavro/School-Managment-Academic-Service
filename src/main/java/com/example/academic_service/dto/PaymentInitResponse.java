package com.example.academic_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitResponse {
    private Long paymentId;
    private String tranId;
    /** Hosted SSLCommerz page URL — frontend opens this in a new tab. */
    private String gatewayPageUrl;
}
