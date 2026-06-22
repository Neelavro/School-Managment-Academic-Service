package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.dto.AccountingSettingsRequest;
import com.example.academic_service.dto.AccountingSettingsResponse;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.AccountingSettingsService;
import com.example.academic_service.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounting/settings")
@RequiredArgsConstructor
public class AccountingSettingsController {

    private final AccountingSettingsService service;

    @GetMapping
    @RequirePermission(submodule = Submodule.ACCOUNTS_SETTINGS, action = "READ")
    public ResponseEntity<ApiResponse<AccountingSettingsResponse>> get() {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.get()));
    }

    @PutMapping
    @RequirePermission(submodule = Submodule.ACCOUNTS_SETTINGS, action = "UPDATE")
    public ResponseEntity<ApiResponse<AccountingSettingsResponse>> update(
            @Valid @RequestBody AccountingSettingsRequest req) {
        return ResponseEntity.ok(new ApiResponse<>("Settings updated", service.update(req)));
    }
}
