package com.example.academic_service.dto.duty_dtos;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TeacherPeriodDutyBulkRequestDto {
    @NotEmpty
    private List<Integer> classRoutineIds;
}
