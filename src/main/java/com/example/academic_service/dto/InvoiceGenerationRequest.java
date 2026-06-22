package com.example.academic_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class InvoiceGenerationRequest {
    /**
     * Any date within the target month — the service normalizes to the 1st.
     */
    @NotNull
    private LocalDate billingPeriod;

    /** Optional academic year filter. Null = all years. */
    private Integer academicYearId;

    /** Optional class ids filter. Null/empty = all classes. */
    private List<Long> classIds;
}
