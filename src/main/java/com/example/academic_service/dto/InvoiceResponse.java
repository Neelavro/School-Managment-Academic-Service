package com.example.academic_service.dto;

import com.example.academic_service.entity.Enrollment;
import com.example.academic_service.entity.Student;
import java.util.Map;

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

    // For the fee slip.
    private String shiftName;
    private String sectionName;
    private String groupName;
    private Integer classRoll;
    private String fatherName;
    private String contactPhone;
    /** Late fee: the amount added (lateFeeApplied) or that will be added after the due date. Null = none. */
    private BigDecimal lateFeeAmount;
    private boolean lateFeeApplied;

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
        r.lateFeeApplied = inv.getLateFeeAmount() != null;
        r.lateFeeAmount = inv.getLateFeeAmount();
        return r;
    }

    /**
     * Student, class and late fee details for the fee slip. upcomingLateFeeByClass is
     * the current late fee per class, shown on fees that don't carry one yet.
     */
    public static InvoiceResponse from(Invoice inv, Enrollment e, List<InvoiceLineResponse> lines,
                                       Map<Integer, BigDecimal> upcomingLateFeeByClass) {
        Student st = e != null ? e.getStudent() : null;
        InvoiceResponse r = from(inv,
                st != null ? st.getStudentSystemId() : null,
                st != null ? st.getNameEnglish() : null,
                e != null && e.getStudentClass() != null ? e.getStudentClass().getName() : null,
                lines);
        if (e != null) {
            r.shiftName = e.getShift() != null ? e.getShift().getName() : null;
            r.sectionName = e.getSection() != null ? e.getSection().getSectionName() : null;
            r.groupName = e.getStudentGroup() != null ? e.getStudentGroup().getGroupName() : null;
            r.classRoll = e.getClassRoll() != null ? e.getClassRoll() : (st != null ? st.getClassRoll() : null);
        }
        if (st != null) {
            r.fatherName = st.getFatherNameEnglish();
            r.contactPhone = firstNonBlank(st.getGuardianPhone(), st.getFatherPhone(), st.getMotherPhone());
        }
        if (!r.lateFeeApplied && e != null && e.getStudentClass() != null && upcomingLateFeeByClass != null
                && inv.getStatus() != InvoiceStatus.CANCELLED) {
            r.lateFeeAmount = upcomingLateFeeByClass.get(e.getStudentClass().getId());
        }
        return r;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }
}
