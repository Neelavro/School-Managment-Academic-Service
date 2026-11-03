package com.example.academic_service.dto.schedule_dtos;

import com.example.academic_service.entity.CalendarEntryType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AcademicCalendarEntryRequestDto {

    @NotNull(message = "Academic year is required")
    private Integer academicYearId;

    @NotNull(message = "Entry type is required")
    private CalendarEntryType type;

    /** Required, except for Exam Days, whose name is the exam routine's title. */
    private String name;

    /** Exam Days: the exam routine (same academic year) these dates are for. */
    private Integer examRoutineId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;
}
