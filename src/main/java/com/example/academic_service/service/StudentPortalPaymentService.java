package com.example.academic_service.service;

import com.example.academic_service.dto.*;
import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Student-portal payments. Every method is scoped by the studentSystemId taken
 * from the caller's JWT — a student can never see or pay another student's invoices.
 *
 * Reuses the admin-side PaymentService for the actual SSLCommerz init / journal
 * posting logic; this class only enforces the ownership check.
 */
@Service
@RequiredArgsConstructor
public class StudentPortalPaymentService {

    private final EnrollmentRepository enrollmentRepo;
    private final InvoiceRepository invoiceRepo;
    private final InvoiceLineRepository invoiceLineRepo;
    private final PaymentRepository paymentRepo;
    private final PaymentService paymentService;

    /**
     * Lists invoices for the logged-in student.
     * If academicYearId is null, defaults to all years (current + past).
     * If status is null, returns all statuses.
     */
    public List<InvoiceResponse> getMyInvoices(String studentSystemId,
                                                InvoiceStatus statusFilter,
                                                Integer academicYearId) {
        List<Enrollment> enrollments = enrollmentRepo.findByStudentSystemId(studentSystemId);
        if (enrollments.isEmpty()) return List.of();
        List<Long> enrollmentIds = enrollments.stream()
                .filter(e -> academicYearId == null
                        || (e.getAcademicYear() != null
                            && Objects.equals(e.getAcademicYear().getId(), academicYearId)))
                .map(Enrollment::getId).toList();
        if (enrollmentIds.isEmpty()) return List.of();

        // Pull all invoices for these enrollments — typical student has < 100 lifetime.
        List<Invoice> invoices = new ArrayList<>();
        for (Long enrollmentId : enrollmentIds) {
            Page<Invoice> page = invoiceRepo.search(enrollmentId, null, statusFilter,
                    PageRequest.of(0, 500));
            invoices.addAll(page.getContent());
        }
        invoices.sort((a, b) -> b.getBillingPeriod().compareTo(a.getBillingPeriod()));

        // Hydrate with line items + student/class info.
        return invoices.stream().map(this::hydrate).toList();
    }

    /**
     * Initiates an SSLCommerz session for one of THIS student's invoices.
     * Verifies the invoice belongs to the student before delegating to PaymentService.
     */
    public PaymentInitResponse initMyPayment(String studentSystemId,
                                              PaymentInitRequest req,
                                              String ipAddress) {
        if (req.getInvoiceId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invoiceId is required");
        }
        Invoice inv = invoiceRepo.findById(req.getInvoiceId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        Enrollment enrollment = enrollmentRepo.findById(inv.getEnrollmentId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice enrollment not found"));
        if (!studentSystemId.equals(enrollment.getStudentSystemId())) {
            // Don't leak the existence of the invoice — same 404 we'd give for nonexistent.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found");
        }
        return paymentService.initOnlinePayment(req,
                "student:" + studentSystemId, ipAddress);
    }

    /**
     * Payment history for the logged-in student — all statuses from the payments table.
     * Failures are intentionally NOT shown (they live in a separate audit table).
     */
    public Page<PaymentResponse> getMyPayments(String studentSystemId, int page, int size) {
        List<Enrollment> enrollments = enrollmentRepo.findByStudentSystemId(studentSystemId);
        if (enrollments.isEmpty()) {
            return Page.empty(PageRequest.of(0, size));
        }
        // Collect all invoice IDs across all enrollments.
        List<Long> enrollmentIds = enrollments.stream().map(Enrollment::getId).toList();
        Set<Long> invoiceIds = new HashSet<>();
        for (Long eid : enrollmentIds) {
            invoiceRepo.search(eid, null, null, PageRequest.of(0, 500))
                    .forEach(i -> invoiceIds.add(i.getId()));
        }
        if (invoiceIds.isEmpty()) {
            return Page.empty(PageRequest.of(0, size));
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));
        // For simplicity, fetch all payments per invoice and combine in-memory.
        // For a student with <100 lifetime invoices this is trivial.
        List<Payment> all = new ArrayList<>();
        for (Long iid : invoiceIds) {
            all.addAll(paymentRepo.findByInvoiceIdOrderByIdDesc(iid));
        }
        all.sort((a, b) -> b.getInitiatedAt().compareTo(a.getInitiatedAt()));

        int total = all.size();
        int from = (int) Math.min((long) page * size, total);
        int to = (int) Math.min((long) from + size, total);
        List<PaymentResponse> sub = all.subList(from, to).stream()
                .map(p -> {
                    Invoice inv = invoiceRepo.findById(p.getInvoiceId()).orElse(null);
                    String invNum = inv != null ? inv.getInvoiceNumber() : null;
                    String name = null;
                    if (inv != null) {
                        Enrollment e = enrollmentRepo.findById(inv.getEnrollmentId()).orElse(null);
                        if (e != null && e.getStudent() != null) name = e.getStudent().getNameEnglish();
                    }
                    return PaymentResponse.from(p, invNum, name);
                })
                .toList();
        return new org.springframework.data.domain.PageImpl<>(sub, pageable, total);
    }

    private InvoiceResponse hydrate(Invoice inv) {
        List<InvoiceLine> lines = invoiceLineRepo.findByInvoiceId(inv.getId());
        List<InvoiceLineResponse> lineDtos = lines.stream().map(InvoiceLineResponse::from).toList();
        String studentSystemId = null, studentName = null, className = null;
        Enrollment e = enrollmentRepo.findById(inv.getEnrollmentId()).orElse(null);
        if (e != null) {
            if (e.getStudent() != null) {
                studentSystemId = e.getStudent().getStudentSystemId();
                studentName = e.getStudent().getNameEnglish();
            }
            if (e.getStudentClass() != null) {
                className = e.getStudentClass().getName();
            }
        }
        return InvoiceResponse.from(inv, studentSystemId, studentName, className, lineDtos);
    }
}
