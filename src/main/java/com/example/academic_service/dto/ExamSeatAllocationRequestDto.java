package com.example.academic_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExamSeatAllocationRequestDto {

    @NotNull
    private Integer examSessionId;

    private Integer sectionId;

    private Integer genderSectionId;

    private Integer roomId;

    private Integer startRoll;

    private Integer endRoll;
}
