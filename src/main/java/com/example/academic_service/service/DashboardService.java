package com.example.academic_service.service;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.DashboardSummaryResponseDto;

public interface DashboardService {
    ApiResponse<DashboardSummaryResponseDto> getSummary();
}
