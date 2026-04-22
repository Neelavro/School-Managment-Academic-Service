package com.example.academic_service.dto.admit_card_dtos;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class AdmitCardRoutineResponseDto {
    private Integer       id;
    private String        title;
    private String        examTypeName;
    private String        academicYearName;
    private String        status;
    private Boolean       isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private List<AdmitCardSessionDto> sessions;
}