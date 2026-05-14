package com.example.academic_service.dto.schedule_dtos;

import com.example.academic_service.entity.CalendarEntryType;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;
}
