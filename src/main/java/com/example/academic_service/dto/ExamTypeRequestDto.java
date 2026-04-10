package com.example.academic_service.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
public class ExamTypeRequestDto {
    @NotBlank(message = "Exam type name is required")
    private String name;

    private Integer orderIndex; // optional
}