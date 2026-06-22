package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.dto.dashboard.*;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.FinanceDashboardService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/accounting/dashboard")
@RequiredArgsConstructor
public class FinanceDashboardController {

    private final FinanceDashboardService service;

    @GetMapping("/summary")
    @RequirePermission(submodule = Submodule.ACCOUNTS_DASHBOARD, action = "READ")
    public ResponseEntity<ApiResponse<FinanceSummaryDto>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate period) {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.getSummary(period)));
    }

    @GetMapping("/payment-status")
    @RequirePermission(submodule = Submodule.ACCOUNTS_DASHBOARD, action = "READ")
    public ResponseEntity<ApiResponse<PaymentStatusBreakdownDto>> paymentStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate period) {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.getPaymentStatusBreakdown(period)));
    }

    @GetMapping("/collection-trend")
    @RequirePermission(submodule = Submodule.ACCOUNTS_DASHBOARD, action = "READ")
    public ResponseEntity<ApiResponse<CollectionTrendDto>> collectionTrend(
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.getCollectionTrend(months)));
    }

    @GetMapping("/top-defaulters")
    @RequirePermission(submodule = Submodule.ACCOUNTS_DASHBOARD, action = "READ")
    public ResponseEntity<ApiResponse<TopDefaultersDto>> topDefaulters(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.getTopDefaulters(limit)));
    }

    @GetMapping("/recent-payments")
    @RequirePermission(submodule = Submodule.ACCOUNTS_DASHBOARD, action = "READ")
    public ResponseEntity<ApiResponse<RecentPaymentsDto>> recentPayments(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.getRecentPayments(limit)));
    }
}
