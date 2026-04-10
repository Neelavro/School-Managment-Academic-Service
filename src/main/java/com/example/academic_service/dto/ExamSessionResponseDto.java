package com.example.academic_service.dto;

import com.example.academic_service.entity.Class;
import com.example.academic_service.entity.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ExamSessionResponseDto {
    private Integer id;
    private ExamRoutine examRoutine;
    private Class examClass;
    private Subject subject;
    private StudentGroup group;        // ← add
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDateTime lastModifiedAt;
    private Boolean isActive;

    public static ExamSessionResponseDto from(ExamSession session) {
        ExamSessionResponseDto dto = new ExamSessionResponseDto();
        dto.setId(session.getId());
        dto.setExamRoutine(session.getExamRoutine());
        dto.setExamClass(session.getExamClass());
        dto.setSubject(session.getSubject());
        dto.setGroup(session.getGroup());              // ← add
        dto.setDate(session.getDate());
        dto.setStartTime(session.getStartTime());
        dto.setEndTime(session.getEndTime());
        dto.setLastModifiedAt(session.getLastModifiedAt());
        dto.setIsActive(session.getIsActive());
        return dto;
    }
}