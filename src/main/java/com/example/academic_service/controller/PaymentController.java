package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.dto.*;
import com.example.academic_service.entity.PaymentMethod;
import com.example.academic_service.entity.PaymentStatus;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.PaymentService;
import com.example.academic_service.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;

    // ── Init ──────────────────────────────────────────────────────────────

    @PostMapping("/init")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYMENTS, action = "CREATE")
    public ResponseEntity<ApiResponse<PaymentInitResponse>> init(
            @Valid @RequestBody PaymentInitRequest req,
            HttpServletRequest httpReq) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Session created",
                        service.initOnlinePayment(req, currentUser(), clientIp(httpReq))));
    }

    // ── IPN webhook — PUBLIC ──────────────────────────────────────────────

    /**
     * SSLCommerz Instant Payment Notification webhook.
     * Permitted publicly in SecurityConfig — security comes from us re-validating
     * the val_id against SSLCommerz's Validation API (we never trust the IPN payload
     * directly). Always returns 200 so SSLCommerz doesn't keep retrying.
     */
    @PostMapping(value = "/ipn", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> ipn(@RequestParam Map<String, String> formData) {
        try {
            service.handleIpn(formData);
        } catch (Exception ignored) {
            // Swallow — we never want SSLCommerz to retry an IPN we couldn't handle.
        }
        return ResponseEntity.ok("OK");
    }

    // ── Reads ─────────────────────────────────────────────────────────────

    @GetMapping
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYMENTS, action = "READ")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> list(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false) Long invoiceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Page<PaymentResponse> result = service.search(status, method, invoiceId, from, to, page, size);
        return ResponseEntity.ok(new ApiResponse<>("OK", PagedResponse.of(result)));
    }

    @GetMapping("/{id}")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYMENTS, action = "READ")
    public ResponseEntity<ApiResponse<PaymentResponse>> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.getOne(id)));
    }

    @PostMapping("/{id}/mark-failed")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYMENTS, action = "CREATE")
    public ResponseEntity<ApiResponse<Void>> markFailed(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        service.markStuckAsFailed(id, reason);
        return ResponseEntity.ok(new ApiResponse<>("Marked as failed", null));
    }

    // ── In-Person Cash Payment ────────────────────────────────────────────

    /** Records an in-person cash payment at the school's accounts desk. */
    @PostMapping("/cash")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYMENTS, action = "CREATE")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordCash(
            @Valid @RequestBody CashPaymentRequest req,
            HttpServletRequest httpReq) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Cash payment recorded",
                        service.recordCashPayment(req, currentUser(), clientIp(httpReq))));
    }

    // ── Failures (separate table) ─────────────────────────────────────────

    @GetMapping("/failures")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYMENTS, action = "READ")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentFailureResponse>>> listFailures(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) Long invoiceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Page<PaymentFailureResponse> result = service.searchFailures(status, invoiceId, from, to, page, size);
        return ResponseEntity.ok(new ApiResponse<>("OK", PagedResponse.of(result)));
    }

    @GetMapping("/failures/{id}")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYMENTS, action = "READ")
    public ResponseEntity<ApiResponse<PaymentFailureResponse>> getFailure(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.getFailure(id)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private String clientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real;
        return req.getRemoteAddr();
    }
}
