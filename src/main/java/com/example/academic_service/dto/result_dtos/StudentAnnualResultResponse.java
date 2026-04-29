package com.example.academic_service.dto.result_dtos;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class StudentAnnualResultResponse {
    private Long enrollmentId;
    private String studentSystemId;
    private String studentName;
    private Integer classRoll;
    private String className;
    private Integer academicYearId;
    private String academicYearName;
    private boolean useGpaForResult;
    private List<SubjectResult> subjectResults;
    private BigDecimal totalMarksRaw;
    private BigDecimal totalMarksScaled;
    private Double overallGpa;
    private boolean passed;

    @Getter
    @Setter
    public static class SubjectResult {
        private Integer subjectId;
        private String subjectName;
        private boolean fourthSubject;
        private BigDecimal marksRaw;
        private Integer maxMarksRaw;
        private BigDecimal marksScaled;
        private String gradeName;
        private Double gpaValue;
        private boolean passed;
        private boolean appeared;
    }
}
