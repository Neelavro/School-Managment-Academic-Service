package com.example.academic_service.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinanceSummaryDto {
    private LocalDate period;
    private long totalActiveEnrollments;
    private BigDecimal totalInvoiced;
    private BigDecimal totalCollected;
    private BigDecimal totalOutstanding;
    /** Collection rate as a percentage, e.g. 87.5. */
    private BigDecimal collectionRate;

    private long fullyPaidCount;
    private long partiallyPaidCount;
    private long pendingCount;
    private long overdueCount;

    private BigDecimal fullyPaidAmount;
    private BigDecimal partiallyPaidAmount;
    private BigDecimal pendingAmount;
    private BigDecimal overdueAmount;
}
