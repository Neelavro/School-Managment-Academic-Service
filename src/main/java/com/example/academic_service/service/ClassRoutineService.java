package com.example.academic_service.service;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.schedule_dtos.ClassRoutineRequestDto;
import com.example.academic_service.dto.schedule_dtos.ClassRoutineResponseDto;
import com.example.academic_service.entity.RoutineType;

import java.util.List;

public interface ClassRoutineService {
    ApiResponse<ClassRoutineResponseDto> create(ClassRoutineRequestDto dto);
    ApiResponse<ClassRoutineResponseDto> update(Integer id, ClassRoutineRequestDto dto);
    ApiResponse<List<ClassRoutineResponseDto>> getAll(RoutineType routineType);
    ApiResponse<Void> delete(Integer id);
}
