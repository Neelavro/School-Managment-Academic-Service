package com.example.academic_service.dto.grading_dtos;

import lombok.*;

import java.util.List;

@Data
@Builder
public class GradingPolicyResponseDto {
    private Long id;
    private String name;
    private Boolean isActive;
    private List<GradeResponseDto> grades;
}