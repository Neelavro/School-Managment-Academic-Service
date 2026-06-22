package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.dto.InvoiceGenerationRequest;
import com.example.academic_service.dto.InvoiceGenerationResult;
import com.example.academic_service.dto.InvoiceResponse;
import com.example.academic_service.entity.InvoiceStatus;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.InvoiceService;
import com.example.academic_service.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/accounting/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService service;

    @GetMapping
    @RequirePermission(submodule = Submodule.ACCOUNTS_INVOICES, action = "READ")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> search(
            @RequestParam(required = false) Long enrollmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate period,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(new ApiResponse<>("OK",
                service.search(enrollmentId, period, status, page, size)));
    }

    @GetMapping("/{id}")
    @RequirePermission(submodule = Submodule.ACCOUNTS_INVOICES, action = "READ")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.getOne(id)));
    }

    @PostMapping("/generate")
    @RequirePermission(submodule = Submodule.ACCOUNTS_INVOICES, action = "CREATE")
    public ResponseEntity<ApiResponse<InvoiceGenerationResult>> generate(
            @Valid @RequestBody InvoiceGenerationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Generation complete", service.generate(req, currentUser())));
    }

    @PostMapping("/{id}/cancel")
    @RequirePermission(submodule = Submodule.ACCOUNTS_INVOICES, action = "DELETE")
    public ResponseEntity<ApiResponse<InvoiceResponse>> cancel(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(new ApiResponse<>("Invoice cancelled",
                service.cancel(id, reason, currentUser())));
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
