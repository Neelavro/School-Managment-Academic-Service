package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.DashboardSummaryResponseDto;
import com.example.academic_service.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponseDto>> summary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }
}
