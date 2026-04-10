package com.example.academic_service.service;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.ExamRoutineRequestDto;
import com.example.academic_service.entity.ExamRoutine;

import java.util.List;

public interface ExamRoutineService {
    ApiResponse<ExamRoutine> create(ExamRoutineRequestDto dto);
    ApiResponse<ExamRoutine> update(Integer id, ExamRoutineRequestDto dto);
    ApiResponse<ExamRoutine> getById(Integer id);
    ApiResponse<List<ExamRoutine>> getAll(Boolean active);
    ApiResponse<List<ExamRoutine>> getByAcademicYear(Integer academicYearId);
    ApiResponse<List<ExamRoutine>> getByExamType(Integer examTypeId);
    ApiResponse<ExamRoutine> publish(Integer id);
    ApiResponse<ExamRoutine> unpublish(Integer id);  // missing
    ApiResponse<ExamRoutine> reactivate(Integer id);
    ApiResponse<Void> delete(Integer id);
}