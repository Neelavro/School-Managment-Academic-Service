package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.InvoiceResponse;
import com.example.academic_service.dto.PaymentInitRequest;
import com.example.academic_service.dto.PaymentInitResponse;
import com.example.academic_service.dto.PaymentResponse;
import com.example.academic_service.dto.student_portal_dtos.StudentPortalProfileDto;
import com.example.academic_service.dto.student_portal_dtos.UpcomingExamDto;
import com.example.academic_service.entity.InvoiceStatus;
import com.example.academic_service.repository.AcademicYearRepository;
import com.example.academic_service.service.ProgressReportPdfService;
import com.example.academic_service.service.ResultPublishService;
import com.example.academic_service.service.StudentPortalPaymentService;
import com.example.academic_service.service.StudentPortalService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student-portal")
@RequiredArgsConstructor
public class StudentPortalController {

    private final StudentPortalService service;
    private final StudentPortalPaymentService paymentService;
    private final ResultPublishService resultPublishService;
    private final ProgressReportPdfService progressReportPdfService;
    private final AcademicYearRepository academicYearRepository;

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

    /**
     * The stored result, sent as-is: one indexed read, no computation or serialization.
     * The ETag changes whenever the stored row is rewritten (republish or update), so the app can revalidate
     * with If-None-Match and get a body-less 304.
     */
    @GetMapping("/result")
    public ResponseEntity<String> getMyResult(
            @RequestParam Integer examRoutineId,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        ResultPublishService.StoredResult stored = resultPublishService
                .findForStudent(resolveStudentSystemId(), examRoutineId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Results for this routine have not been released yet"));
        String etag = "\"r" + stored.version() + "\"";
        CacheControl cache = CacheControl.noCache().cachePrivate();
        if (etag.equals(ifNoneMatch))
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).cacheControl(cache).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .eTag(etag)
                .cacheControl(cache)
                .body("{\"message\":\"Result fetched\",\"data\":" + stored.json() + "}");
    }

    /**
     * The student's progress-report data for a routine, stored at publish (see ResultPublishService):
     * subjects, components, highest marks, positions and the grading table. The portal draws the PDF.
     */
    @GetMapping("/progress-report")
    public ResponseEntity<String> getMyProgressReport(@RequestParam Integer examRoutineId) {
        ResultPublishService.StoredResult stored = resultPublishService
                .findReportForStudent(resolveStudentSystemId(), examRoutineId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Results for this routine have not been released yet"));
        if (stored.json() == null)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The progress report is still being prepared. Please try again in a few minutes.");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.noCache().cachePrivate())
                .body("{\"message\":\"Progress report fetched\",\"data\":" + stored.json() + "}");
    }

    // ── Grand Final (published per class like an exam; see ResultPublishService.grandFinalKey) ──

    /** The years whose Grand Final is published for this student: [{academicYearId, academicYearName}], newest first. */
    @GetMapping("/grand-finals")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyGrandFinals() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Integer yearId : resultPublishService.grandFinalYearsForStudent(resolveStudentSystemId())) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("academicYearId", yearId);
            m.put("academicYearName", academicYearRepository.findById(yearId).map(y -> y.getYearName()).orElse(null));
            out.add(m);
        }
        return ResponseEntity.ok(ApiResponse.success("Grand Finals fetched", out));
    }

    /** The stored Grand Final result, sent as-is (ETag like /result). */
    @GetMapping("/grand-final")
    public ResponseEntity<String> getMyGrandFinal(
            @RequestParam Integer academicYearId,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        ResultPublishService.StoredResult stored = resultPublishService
                .findForStudent(resolveStudentSystemId(), ResultPublishService.grandFinalKey(academicYearId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "The Grand Final has not been released yet"));
        String etag = "\"g" + stored.version() + "\"";
        CacheControl cache = CacheControl.noCache().cachePrivate();
        if (etag.equals(ifNoneMatch))
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).cacheControl(cache).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .eTag(etag)
                .cacheControl(cache)
                .body("{\"message\":\"Grand Final fetched\",\"data\":" + stored.json() + "}");
    }

    /** The student's Grand Final transcript as a PDF, printed from the page stored at publish. */
    @GetMapping("/grand-final/transcript")
    public ResponseEntity<byte[]> getMyGrandFinalTranscript(@RequestParam Integer academicYearId) throws Exception {
        String sid = resolveStudentSystemId();
        ResultPublishService.StoredResult stored = resultPublishService
                .findReportForStudent(sid, ResultPublishService.grandFinalKey(academicYearId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "The Grand Final has not been released yet"));
        if (stored.json() == null || stored.json().isBlank())
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The transcript is still being prepared. Please try again in a few minutes.");
        StudentPortalProfileDto profile = service.getProfile(sid).getData();
        byte[] pdf = progressReportPdfService.printStoredGrandFinal(stored.json(), profile != null ? profile.getImageUrl() : null);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"grand-final-" + sid + ".pdf\"")
                .cacheControl(CacheControl.noStore())
                .body(pdf);
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
