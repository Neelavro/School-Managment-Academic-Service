package com.example.academic_service.dto.exam_dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImportSessionsRequestDto {

    @NotNull(message = "Source routine is required")
    private Integer sourceRoutineId;

    @NotNull(message = "Target routine is required")
    private Integer targetRoutineId;
}
