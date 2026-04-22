package com.example.academic_service.dto.marking_dtos;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class StudentMarkResponse {
    private Long id;
    private Long enrollmentId;
    private Integer examSessionId;
    private Integer examComponentId;
    private String examComponentName;
    private BigDecimal marksObtained;
}