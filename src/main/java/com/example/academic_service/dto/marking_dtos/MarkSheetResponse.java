// response
package com.example.academic_service.dto.marking_dtos;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class MarkSheetResponse {
    private Integer routineId;
    private Integer subjectId;
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
        private String nameEnglish;
        private Integer classRoll;
        private Integer groupId;
        private String groupName;
        private List<MarkEntry> marks;
        private BigDecimal total;
        private String status; // null / "PRESENT" / "ABSENT" / "EXPELLED"
        private boolean isFourthSubject;

        @Getter
        @Setter
        public static class MarkEntry {
            private Integer examComponentId;
            private String examComponentName;
            private BigDecimal marksObtained; // null if not entered yet
        }
    }
}
