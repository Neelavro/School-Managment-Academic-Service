package com.example.academic_service.dto.result_dtos;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class AnnualResultResponse {
    private Integer academicYearId;
    private String academicYearName;
    private String className;
    private boolean useGpaForResult;
    private List<SubjectInfo> subjects;
    private List<StudentResultRow> students;

    @Getter
    @Setter
    public static class SubjectInfo {
        private Integer subjectId;
        private String subjectName;
        private boolean fourthSubject;
    }

    @Getter
    @Setter
    public static class StudentResultRow {
        private Long enrollmentId;
        private String studentSystemId;
        private String studentName;
        private Integer classRoll;
        private Integer genderSectionId;
        private String genderSectionName;
        private Long sectionId;
        private String sectionName;
        private Integer groupId;
        private String groupName;
        private List<SubjectResult> subjectResults;
        private BigDecimal totalMarksRaw;
        private BigDecimal totalMarksScaled;
        private Double overallGpa;
        private boolean passed;
        private Merit merit;
    }

    @Getter
    @Setter
    public static class Merit {
        private Integer classRank;
        private Integer genderSectionRank;
        private Integer sectionRank;
        private Integer groupRank;
    }

    @Getter
    @Setter
    public static class SubjectResult {
        private Integer subjectId;
        private BigDecimal marksRaw;
        private Integer maxMarksRaw;
        private BigDecimal marksScaled;
        private String gradeName;
        private Double gpaValue;
        private boolean passed;
        private boolean appeared;
        private boolean fourthSubject;
    }
}
