package com.example.academic_service.dto.result_dtos;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class StudentRoutineResultResponse {
    private Long enrollmentId;
    private String studentSystemId;
    private String studentName;
    private Integer classRoll;
    private String className;
    private Integer examRoutineId;
    private String routineTitle;
    private String examTypeName;
    private boolean useGpaForResult;
    private List<SubjectResult> subjectResults;
    private BigDecimal totalMarks;
    private Double overallGpa;
    private boolean passed;
    /** Set only on results stored at publish (the student portal); null elsewhere. */
    private Positions positions;

    /**
     * The student's positions, the same numbers as the admin progress report (MeritRanking), with the
     * ones that don't apply left null: all of them for a failed student, the shift (gender section)
     * position when the class has only one, and the section position when the student has no section.
     */
    @Getter
    @Setter
    public static class Positions {
        private Integer classPosition;
        private Integer shiftPosition;
        private Integer sectionPosition;
    }

    @Getter
    @Setter
    public static class SubjectResult {
        private Integer subjectId;
        private String subjectName;
        private boolean fourthSubject;
        private BigDecimal marksObtained;
        private Integer maxMarks;
        private Integer passMarks;
        private String gradeName;
        private Double gpaValue;
        private boolean passed;
        private boolean appeared;
        private String status; // null = present, "ABSENT", "EXPELLED"
        private List<ComponentMark> components;
    }

    @Getter
    @Setter
    public static class ComponentMark {
        private String componentName;
        private BigDecimal obtained;
        private Integer max;
    }
}
