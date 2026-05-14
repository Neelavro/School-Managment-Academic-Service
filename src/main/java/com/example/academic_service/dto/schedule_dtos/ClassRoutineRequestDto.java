package com.example.academic_service.dto.schedule_dtos;

import com.example.academic_service.entity.RoutineType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
public class ClassRoutineRequestDto {

    @NotNull(message = "Class is required")
    private Integer classId;

    @NotNull(message = "Gender section is required")
    private Integer genderSectionId;

    private Long sectionId;

    private Integer studentGroupId;

    @NotNull(message = "Room is required")
    private Integer roomId;

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Routine type is required")
    private RoutineType routineType;
}
