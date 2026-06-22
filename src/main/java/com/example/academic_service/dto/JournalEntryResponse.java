package com.example.academic_service.dto;

import com.example.academic_service.entity.JournalEntry;
import com.example.academic_service.entity.JournalReferenceType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class JournalEntryResponse {
    private Long id;
    private String entryNumber;
    private LocalDate entryDate;
    private String description;
    private JournalReferenceType referenceType;
    private Long referenceId;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private Boolean isReversed;
    private Long reversesEntryId;
    private Long reversedByEntryId;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<JournalLineResponse> lines;

    public static JournalEntryResponse from(JournalEntry e, List<JournalLineResponse> lines) {
        JournalEntryResponse r = new JournalEntryResponse();
        r.id = e.getId();
        r.entryNumber = e.getEntryNumber();
        r.entryDate = e.getEntryDate();
        r.description = e.getDescription();
        r.referenceType = e.getReferenceType();
        r.referenceId = e.getReferenceId();
        r.totalDebit = e.getTotalDebit();
        r.totalCredit = e.getTotalCredit();
        r.isReversed = e.getIsReversed();
        r.reversesEntryId = e.getReversesEntryId();
        r.reversedByEntryId = e.getReversedByEntryId();
        r.createdBy = e.getCreatedBy();
        r.createdAt = e.getCreatedAt();
        r.lines = lines;
        return r;
    }
}
