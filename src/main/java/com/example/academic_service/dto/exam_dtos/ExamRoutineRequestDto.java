package com.example.academic_service.dto.exam_dtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
@Getter
@Setter
public class ExamRoutineRequestDto {
    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Exam type is required")
    private Integer examTypeId;

    @NotNull(message = "Academic year is required")
    private Integer academicYearId;

    private LocalDate routineStartDate;
    private LocalDate routineEndDate;
    private Boolean considerForAnnualResult = true;

    /** Class days in this exam's term; null clears it. */
    @jakarta.validation.constraints.Min(value = 0, message = "Working days can't be negative")
    @jakarta.validation.constraints.Max(value = 366, message = "Working days can't be more than 366")
    private Integer workingDays;
}