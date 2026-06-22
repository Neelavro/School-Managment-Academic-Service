package com.example.academic_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * Free-form journal voucher — any number of balanced Dr/Cr lines.
 * Validated by JournalEntryService (Dr=Cr, all leaves, exactly one of debit/credit > 0).
 */
@Getter
@Setter
public class JournalVoucherRequest {

    @NotNull
    private LocalDate voucherDate;

    private String narration;

    private String partyName;

    @NotEmpty(message = "At least two lines are required")
    @Valid
    private List<JournalLineRequest> lines;
}
