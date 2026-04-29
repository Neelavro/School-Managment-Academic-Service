package com.example.academic_service.dto.result_dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ResultOverviewResponse {

    private String className;
    private Integer totalEnrolled;

    // routine-specific
    private Integer examRoutineId;
    private String routineTitle;
    private String examTypeName;

    // annual-specific
    private Integer academicYearId;
    private String academicYearName;

    private List<SubjectOverview> subjects;

    @Getter
    @Setter
    public static class SubjectOverview {
        private Integer subjectId;
        private String subjectName;
        private boolean fourthSubject;
        private Integer appeared;
        private Integer absent;
        private Integer passed;
        private Integer failed;
        private Double passRate;
        private Map<String, Integer> gradeDistribution;
    }
}
