package com.example.academic_service.dto;

import lombok.*;
import java.util.List;

// AdmitCardSessionDto.java
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class AdmitCardSessionDto {
    private Integer id;
    private String  subjectName;
    private String  date;
    private String  startTime;
    private String  endTime;
    private String  className;
    private List<AdmitCardAllocationDto> allocations;
    private List<AdmitCardSessionDto>    fullSchedule; // ← add
}