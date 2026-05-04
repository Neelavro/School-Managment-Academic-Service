package com.example.academic_service.dto.exam_dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class BulkSessionUpdateItemDto {

    @NotNull(message = "Session id is required")
    private Integer id;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean showOnAdmitCard;
}
