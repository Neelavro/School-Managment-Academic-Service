package com.example.academic_service.dto.marking_dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarkingStructureComponentRequest {
    private Integer examComponentId;
    private Integer maxMarks;
}