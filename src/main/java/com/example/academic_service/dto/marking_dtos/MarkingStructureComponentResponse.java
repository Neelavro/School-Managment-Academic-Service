package com.example.academic_service.dto.marking_dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarkingStructureComponentResponse {
    private Integer id;
    private Integer examComponentId;
    private String examComponentName;
    private Integer maxMarks;
    private Integer passMarks;
}