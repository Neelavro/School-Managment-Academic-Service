package com.example.academic_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeeCategoryRequest {

    @NotBlank(message = "code is required")
    @Size(max = 50)
    private String code;

    @NotBlank(message = "name is required")
    @Size(max = 255)
    private String name;

    @NotNull(message = "incomeLedgerId is required")
    private Long incomeLedgerId;

    private String description;

    private Boolean isRecurring;

    private Boolean isActive;
}
