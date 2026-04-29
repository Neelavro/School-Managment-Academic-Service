package com.example.academic_service.dto.result_dtos;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class MeritListResponse {
    private Integer academicYearId;
    private String academicYearName;
    private String className;
    private boolean useGpaForResult;
    private List<MeritEntry> entries;

    @Getter
    @Setter
    public static class MeritEntry {
        private Integer rank;
        private Long enrollmentId;
        private String studentSystemId;
        private String studentName;
        private Integer classRoll;
        private BigDecimal totalMarksRaw;
        private BigDecimal totalMarksScaled;
        private Double overallGpa;
        private boolean passed;
    }
}
