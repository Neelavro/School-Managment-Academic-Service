package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.InvoiceResponse;
import com.example.academic_service.dto.PaymentInitRequest;
import com.example.academic_service.dto.PaymentInitResponse;
import com.example.academic_service.dto.PaymentResponse;
import com.example.academic_service.dto.result_dtos.StudentRoutineResultResponse;
import com.example.academic_service.dto.student_portal_dtos.StudentPortalProfileDto;
import com.example.academic_service.dto.student_portal_dtos.UpcomingExamDto;
import com.example.academic_service.entity.InvoiceStatus;
import com.example.academic_service.service.StudentPortalPaymentService;
import com.example.academic_service.service.StudentPortalService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student-portal")
@RequiredArgsConstructor
public class StudentPortalController {

    private final StudentPortalService service;
    private final StudentPortalPaymentService paymentService;

    private String resolveStudentSystemId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        Object raw = details != null ? details.get("studentSystemId") : null;
        if (raw == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a student account");
        return raw.toString();
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<StudentPortalProfileDto>> getProfile() {
        return ResponseEntity.ok(service.getProfile(resolveStudentSystemId()));
    }

    @GetMapping("/weekly-schedule")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWeeklySchedule() {
        return ResponseEntity.ok(service.getWeeklySchedule(resolveStudentSystemId()));
    }

    @GetMapping("/upcoming-exams")
    public ResponseEntity<ApiResponse<List<UpcomingExamDto>>> getUpcomingExams() {
        return ResponseEntity.ok(service.getUpcomingExams(resolveStudentSystemId()));
    }

    @GetMapping("/routines")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAvailableRoutines() {
        return ResponseEntity.ok(service.getAvailableRoutines(resolveStudentSystemId()));
    }

    @GetMapping("/result")
    public ResponseEntity<ApiResponse<StudentRoutineResultResponse>> getMyResult(
            @RequestParam Integer examRoutineId) {
        return ResponseEntity.ok(service.getMyResult(resolveStudentSystemId(), examRoutineId));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.changePassword(
                resolveStudentSystemId(),
                body.get("currentPassword"),
                body.get("newPassword")));
    }

    // ── Fees + Online Payment ─────────────────────────────────────────────

    /** My invoices. Optional filters: status, academicYearId. */
    @GetMapping("/my-invoices")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getMyInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Integer academicYearId) {
        List<InvoiceResponse> invoices = paymentService.getMyInvoices(
                resolveStudentSystemId(), status, academicYearId);
        return ResponseEntity.ok(ApiResponse.success("OK", invoices));
    }

    /** Initiates SSLCommerz session for one of my invoices. Returns gateway URL. */
    @PostMapping("/payments/init")
    public ResponseEntity<ApiResponse<PaymentInitResponse>> initMyPayment(
            @RequestBody PaymentInitRequest req,
            HttpServletRequest httpReq) {
        PaymentInitResponse result = paymentService.initMyPayment(
                resolveStudentSystemId(), req, clientIp(httpReq));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Session created", result));
    }

    /** My payment history (successful + initiated). Failures are not exposed here. */
    @GetMapping("/my-payments")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Page<PaymentResponse> result = paymentService.getMyPayments(
                resolveStudentSystemId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("OK", result));
    }

    private String clientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real;
        return req.getRemoteAddr();
    }
}
