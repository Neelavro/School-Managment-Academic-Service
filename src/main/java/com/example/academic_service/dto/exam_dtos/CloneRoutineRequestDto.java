package com.example.academic_service.dto.exam_dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CloneRoutineRequestDto {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Academic year is required")
    private Integer academicYearId;
}
