package com.example.academic_service.dto.grading_dtos;

import lombok.*;

@Data
@Builder
public class GradeResponseDto {
    private Long id;
    private Long gradingPolicyId;
    private String name;
    private Double gpaValue;
    private Double minMark;
    private Double maxMark;
}