package com.example.academic_service.dto;
import com.example.academic_service.entity.Room;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ExamSessionRequestDto {

    @NotNull(message = "Exam routine is required")
    private Integer examRoutineId;

    @NotNull(message = "Class is required")
    private Integer classId;

    @NotNull(message = "Subject is required")
    private Integer subjectId;

    private Integer group;   // nullable
    private Integer genderSectionId;  // nullable
    private Integer sectionId;        // nullable

    private List<Integer> roomIds;    // nullable — frontend sends [1, 2, 3]

    private Integer startRoll;        // nullable
    private Integer endRoll;          // nullable

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;
}