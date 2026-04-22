// request
package com.example.academic_service.dto.marking_dtos;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class SaveMarksRequest {
    private Integer examSessionId;
    private List<StudentMarkEntry> marks;

    @Getter
    @Setter
    public static class StudentMarkEntry {
        private Long enrollmentId;
        private Integer examComponentId;
        private BigDecimal marksObtained; // nullable for partial save
    }
}