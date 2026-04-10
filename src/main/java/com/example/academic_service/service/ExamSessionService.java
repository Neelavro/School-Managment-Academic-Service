package com.example.academic_service.service;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.ExamSessionRequestDto;
import com.example.academic_service.dto.ExamSessionResponseDto;
import com.example.academic_service.entity.ExamSession;

import java.util.List;

public interface ExamSessionService {
    ApiResponse<ExamSessionResponseDto> create(ExamSessionRequestDto dto);
    ApiResponse<ExamSessionResponseDto> update(Integer id, ExamSessionRequestDto dto);
    ApiResponse<List<ExamSessionResponseDto>> getByRoutine(Integer routineId, Boolean active);
    ApiResponse<ExamSessionResponseDto> reactivate(Integer id);
    ApiResponse<Void> delete(Integer id);
}