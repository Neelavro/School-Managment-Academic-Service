package com.example.academic_service.service;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.exam_dtos.ExamTypeRequestDto;
import com.example.academic_service.entity.ExamType;

import java.util.List;

public interface ExamTypeService {
    ApiResponse<ExamType> create(ExamTypeRequestDto dto);
    ApiResponse<ExamType> update(Integer id, ExamTypeRequestDto dto);
    ApiResponse<List<ExamType>> getAll(Boolean active);
    ApiResponse<ExamType> reactivate(Integer id);
    ApiResponse<Void> delete(Integer id);
}