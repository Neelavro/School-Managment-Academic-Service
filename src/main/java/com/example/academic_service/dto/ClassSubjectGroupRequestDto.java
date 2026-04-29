package com.example.academic_service.dto;
import jakarta.validation.constraints.NotNull;
import lombok.*;


@Getter
@Setter
public class ClassSubjectGroupRequestDto {

    @NotNull(message = "Class is required")
    private Integer classId;

    @NotNull(message = "Subject is required")
    private Integer subjectId;

    private Integer studentGroupId; // null = common subject

    private Boolean isFourthSubject = false;
}