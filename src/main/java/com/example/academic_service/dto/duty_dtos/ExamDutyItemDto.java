package com.example.academic_service.dto.duty_dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExamDutyItemDto {
    @NotNull
    private Integer examSessionId;
    @NotNull
    private Integer roomId;
}
