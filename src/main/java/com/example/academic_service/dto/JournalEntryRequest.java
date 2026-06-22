package com.example.academic_service.dto;

import com.example.academic_service.entity.JournalReferenceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class JournalEntryRequest {
    @NotNull
    private LocalDate entryDate;

    private String description;

    @NotNull
    private JournalReferenceType referenceType;

    private Long referenceId;

    @NotEmpty(message = "At least one line is required")
    @Valid
    private List<JournalLineRequest> lines;
}
