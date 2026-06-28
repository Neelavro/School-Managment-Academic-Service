package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.SchoolPlatformBillsService;
import com.example.academic_service.util.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Surface platform-billing history to the school's own admins.
 * Proxies {@link SchoolPlatformBillsService} which authenticates upstream
 * with the shared PLATFORM_METRICS_SECRET.
 */
@RestController
@RequestMapping("/api/platform-bills")
@RequiredArgsConstructor
public class SchoolPlatformBillsController {

    private final SchoolPlatformBillsService service;

    @GetMapping
    @RequirePermission(submodule = Submodule.ACCOUNTS_SETTINGS, action = "VIEW")
    public ResponseEntity<ApiResponse<JsonNode>> getMyBills() {
        return ResponseEntity.ok(new ApiResponse<>("Platform bills", service.fetchBills()));
    }
}
