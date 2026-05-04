package com.example.academic_service.dto.exam_dtos;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class ExamSessionRequestDto {

    @NotNull(message = "Exam routine is required")
    private Integer examRoutineId;

    @NotNull(message = "Class is required")
    private Integer classId;

    @NotNull(message = "Subject is required")
    private Integer subjectId;

    private Integer group;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    private Boolean showOnAdmitCard = true;
}
