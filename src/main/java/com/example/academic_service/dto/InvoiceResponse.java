package com.example.academic_service.dto;

import com.example.academic_service.entity.Invoice;
import com.example.academic_service.entity.InvoiceStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private Long enrollmentId;
    private String studentSystemId;
    private String studentName;
    private String className;
    private LocalDate billingPeriod;
    private LocalDate issuedDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private InvoiceStatus status;
    private String notes;
    private Long journalEntryId;
    private List<InvoiceLineResponse> lines;
    private LocalDateTime createdAt;

    public static InvoiceResponse from(Invoice inv,
                                       String studentSystemId, String studentName, String className,
                                       List<InvoiceLineResponse> lines) {
        InvoiceResponse r = new InvoiceResponse();
        r.id = inv.getId();
        r.invoiceNumber = inv.getInvoiceNumber();
        r.enrollmentId = inv.getEnrollmentId();
        r.studentSystemId = studentSystemId;
        r.studentName = studentName;
        r.className = className;
        r.billingPeriod = inv.getBillingPeriod();
        r.issuedDate = inv.getIssuedDate();
        r.dueDate = inv.getDueDate();
        r.totalAmount = inv.getTotalAmount();
        r.paidAmount = inv.getPaidAmount();
        r.outstandingAmount = inv.getTotalAmount().subtract(inv.getPaidAmount());
        r.status = inv.getStatus();
        r.notes = inv.getNotes();
        r.journalEntryId = inv.getJournalEntryId();
        r.lines = lines;
        r.createdAt = inv.getCreatedAt();
        return r;
    }
}
