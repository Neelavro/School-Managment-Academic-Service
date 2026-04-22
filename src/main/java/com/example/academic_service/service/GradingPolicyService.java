package com.example.academic_service.service;

import com.example.academic_service.dto.grading_dtos.GradingPolicyRequestDto;
import com.example.academic_service.dto.grading_dtos.GradingPolicyResponseDto;

import java.util.List;

public interface GradingPolicyService {
    GradingPolicyResponseDto create(GradingPolicyRequestDto dto);
    GradingPolicyResponseDto getById(Long id);
    List<GradingPolicyResponseDto> getAll();
    List<GradingPolicyResponseDto> getByActive(Boolean isActive);
    GradingPolicyResponseDto update(Long id, GradingPolicyRequestDto dto);
    void delete(Long id);
}