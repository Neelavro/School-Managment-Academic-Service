package com.example.academic_service.dto.grading_dtos;

import lombok.Data;

@Data
public class GradingPolicyRequestDto {
    private String name;
    private Boolean isActive;
}
