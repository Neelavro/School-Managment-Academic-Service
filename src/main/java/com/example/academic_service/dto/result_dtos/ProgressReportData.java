package com.example.academic_service.dto.result_dtos;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ProgressReportData {

    private Integer examRoutineId;
    private String routineTitle;
    private String examTypeName;
    private String className;
    private String academicYearName;
    private boolean useGpaForResult;

    private List<ComponentInfo> components;
    private List<SubjectInfo> subjects;
    private List<StudentReport> students;

    @Getter
    @Setter
    public static class ComponentInfo {
        private Integer componentId;
        private String componentName;
        private Integer orderIndex;
    }

    @Getter
    @Setter
    public static class SubjectInfo {
        private Integer subjectId;
        private String subjectName;
        private boolean fourthSubject;
        private Integer totalMarks;
        private BigDecimal highestMarks;
        private Map<Integer, Integer> componentMaxMarks; // componentId -> maxMarks
    }

    @Getter
    @Setter
    public static class StudentReport {
        private Long enrollmentId;
        private String studentSystemId;
        private String studentName;
        private String fatherName;
        private String motherName;
        private Integer classRoll;
        private String genderSectionName;
        private String sectionName;
        private String groupName;
        private String imageUrl;
        private List<SubjectResult> subjectResults;
        private BigDecimal totalMarks;
        private Double overallGpa;
        private Double gpaWithout4th;
        private boolean passed;
        private int failedSubjectCount;
        private Integer classRank;
        private Integer genderSectionRank;
        private Integer sectionRank;
        private Integer groupRank;
    }

    @Getter
    @Setter
    public static class SubjectResult {
        private Integer subjectId;
        private Map<Integer, BigDecimal> componentMarks; // componentId -> marks obtained
        private BigDecimal totalMarks;
        private String gradeName;
        private Double gpaValue;
        private boolean passed;
        private boolean appeared;
        private boolean fourthSubject;
    }
}
