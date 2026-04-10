package com.example.academic_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubjectRequestDto {
    @NotBlank(message = "Subject name is required")
    private String name;

    private String code; // optional
}