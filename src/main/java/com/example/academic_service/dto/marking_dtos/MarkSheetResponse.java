// response
package com.example.academic_service.dto.marking_dtos;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class MarkSheetResponse {
    private Integer examSessionId;
    private String subjectName;
    private String examTypeName;
    private String className;
    private Integer totalMarks;
    private List<ComponentInfo> components;
    private List<StudentMarkRow> students;

    @Getter
    @Setter
    public static class ComponentInfo {
        private Integer examComponentId;
        private String examComponentName;
        private Integer maxMarks;
    }

    @Getter
    @Setter
    public static class StudentMarkRow {
        private Long enrollmentId;
        private String studentSystemId;
        private Integer classRoll;
        private List<MarkEntry> marks;
        private BigDecimal total;

        @Getter
        @Setter
        public static class MarkEntry {
            private Integer examComponentId;
            private String examComponentName;
            private BigDecimal marksObtained; // null if not entered yet
        }
    }
}