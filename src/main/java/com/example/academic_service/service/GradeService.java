package com.example.academic_service.service;

import com.example.academic_service.dto.grading_dtos.GradeRequestDto;
import com.example.academic_service.dto.grading_dtos.GradeResponseDto;

import java.util.List;

public interface GradeService {
    GradeResponseDto create(GradeRequestDto dto);
    GradeResponseDto getById(Long id);
    List<GradeResponseDto> getAll();
    GradeResponseDto update(Long id, GradeRequestDto dto);
    void delete(Long id);
}