package com.example.academic_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubjectRequestDto {
    @NotBlank(message = "Subject name is required")
    private String name;

    private String code; // optional

    private Integer orderIndex; // optional; global display order across all classes

    private Integer parentSubjectId; // optional; makes this a sub-subject of that subject
}