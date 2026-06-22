package com.example.academic_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceGenerationResult {
    private LocalDate billingPeriod;
    private int createdCount;
    private int skippedExisting;
    private int skippedNoFees;
    private int failedCount;
    private List<String> warnings = new ArrayList<>();
}
