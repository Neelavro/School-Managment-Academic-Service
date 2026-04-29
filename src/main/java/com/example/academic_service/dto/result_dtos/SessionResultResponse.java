package com.example.academic_service.dto.result_dtos;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class SessionResultResponse {
    private Integer examSessionId;
    private String className;
    private String subjectName;
    private String examTypeName;
    private String examRoutineTitle;
    private Integer totalMarks;
    private Integer passMarks;
    private boolean useGpaForResult;
    private List<ComponentInfo> components;
    private List<StudentSessionResult> students;

    @Getter
    @Setter
    public static class ComponentInfo {
        private Integer examComponentId;
        private String examComponentName;
        private Integer maxMarks;
        private Integer passMarks;
    }

    @Getter
    @Setter
    public static class StudentSessionResult {
        private Long enrollmentId;
        private String studentSystemId;
        private String studentName;
        private Integer classRoll;
        private boolean appeared;
        private List<ComponentMark> componentMarks;
        private BigDecimal marksObtained;
        private Integer maxMarks;
        private String gradeName;
        private Double gpaValue;
        private boolean passed;
    }

    @Getter
    @Setter
    public static class ComponentMark {
        private Integer examComponentId;
        private String examComponentName;
        private BigDecimal marksObtained;
        private Integer maxMarks;
        private Integer passMarks;
        private boolean passed;
    }
}
