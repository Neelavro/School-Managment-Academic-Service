package com.example.academic_service.dto.marking_dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExamComponentResponse {
    private Integer id;
    private String name;
    private Integer orderIndex;
    private Boolean isActive;
}