package com.example.academic_service.dto.admit_card_dtos;

import lombok.*;
import java.util.List;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class AdmitCardAllocationDto {
    private Integer id;
    private Integer roomId;
    private String  roomName;
    private Integer startRoll;
    private Integer endRoll;
    private String  sectionName;
    private String  genderSectionName;
    private List<AdmitCardStudentDto> students;
}