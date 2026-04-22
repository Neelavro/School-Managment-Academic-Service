package com.example.academic_service.service;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.exam_dtos.ExamSessionRequestDto;
import com.example.academic_service.dto.exam_dtos.ExamSessionResponseDto;

import java.util.List;

public interface ExamSessionService {
    ApiResponse<ExamSessionResponseDto> create(ExamSessionRequestDto dto);
    ApiResponse<ExamSessionResponseDto> update(Integer id, ExamSessionRequestDto dto);
    ApiResponse<List<ExamSessionResponseDto>> getByRoutine(Integer routineId, Boolean active);
    ApiResponse<ExamSessionResponseDto> reactivate(Integer id);
    ApiResponse<Void> delete(Integer id);
}