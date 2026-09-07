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
    private List<RoutineInfo> routines;
    private List<SubjectResult> subjectResults;
    private BigDecimal totalMarksRaw;
    private BigDecimal totalMarksScaled;
    private Double overallGpa;
    private boolean passed;

    @Getter
    @Setter
    public static class RoutineInfo {
        private Integer routineId;
        private String routineTitle;
        private String examTypeName;
    }

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
        private String status; // null = present, "ABSENT", "EXPELLED"
        private List<RoutineBreakdown> routineBreakdowns;
    }

    @Getter
    @Setter
    public static class RoutineBreakdown {
        private Integer routineId;
        private BigDecimal marksObtained;
        private Integer maxMarks;
        private String gradeName;
        private Double gpaValue;
        private boolean passed;
        private boolean appeared;
        private String status; // null = present, "ABSENT", "EXPELLED"
    }
}
