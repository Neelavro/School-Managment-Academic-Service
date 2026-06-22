package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.dto.JournalVoucherRequest;
import com.example.academic_service.dto.SimpleVoucherRequest;
import com.example.academic_service.dto.VoucherResponse;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.entity.VoucherType;
import com.example.academic_service.service.VoucherService;
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
@RequestMapping("/api/accounting/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService service;

    @GetMapping
    @RequirePermission(submodule = Submodule.ACCOUNTS_VOUCHERS, action = "READ")
    public ResponseEntity<ApiResponse<Page<VoucherResponse>>> search(
            @RequestParam(required = false) VoucherType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.search(type, from, to, page, size)));
    }

    @GetMapping("/{id}")
    @RequirePermission(submodule = Submodule.ACCOUNTS_VOUCHERS, action = "READ")
    public ResponseEntity<ApiResponse<VoucherResponse>> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.getOne(id)));
    }

    @PostMapping("/receipt")
    @RequirePermission(submodule = Submodule.ACCOUNTS_VOUCHERS, action = "CREATE")
    public ResponseEntity<ApiResponse<VoucherResponse>> postReceipt(
            @Valid @RequestBody SimpleVoucherRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Receipt voucher posted", service.postReceipt(req, currentUser())));
    }

    @PostMapping("/payment")
    @RequirePermission(submodule = Submodule.ACCOUNTS_VOUCHERS, action = "CREATE")
    public ResponseEntity<ApiResponse<VoucherResponse>> postPayment(
            @Valid @RequestBody SimpleVoucherRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Payment voucher posted", service.postPayment(req, currentUser())));
    }

    @PostMapping("/contra")
    @RequirePermission(submodule = Submodule.ACCOUNTS_VOUCHERS, action = "CREATE")
    public ResponseEntity<ApiResponse<VoucherResponse>> postContra(
            @Valid @RequestBody SimpleVoucherRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Contra voucher posted", service.postContra(req, currentUser())));
    }

    @PostMapping("/journal")
    @RequirePermission(submodule = Submodule.ACCOUNTS_VOUCHERS, action = "CREATE")
    public ResponseEntity<ApiResponse<VoucherResponse>> postJournal(
            @Valid @RequestBody JournalVoucherRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Journal voucher posted", service.postJournal(req, currentUser())));
    }

    @PostMapping("/{id}/reverse")
    @RequirePermission(submodule = Submodule.ACCOUNTS_VOUCHERS, action = "CREATE")
    public ResponseEntity<ApiResponse<VoucherResponse>> reverse(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Voucher reversed", service.reverse(id, reason, currentUser())));
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
