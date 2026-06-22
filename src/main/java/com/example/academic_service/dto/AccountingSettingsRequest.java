package com.example.academic_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccountingSettingsRequest {
    private Long arAccountId;
    private Long cashAccountId;

    // ── Online payment posting ────────────────────────────────────────────
    private Long gatewayClearingAccountId;
    private Long platformFeeAccountId;
    private Long platformPayableAccountId;

    @DecimalMin(value = "0", message = "platformFeePercent must be >= 0")
    private BigDecimal platformFeePercent;

    @DecimalMin(value = "0", message = "platformFeeFlat must be >= 0")
    private BigDecimal platformFeeFlat;

    @Min(value = 0, message = "invoiceDueDays must be non-negative")
    private Integer invoiceDueDays;

    private String invoiceNumberPrefix;
    private String journalNumberPrefix;
}
