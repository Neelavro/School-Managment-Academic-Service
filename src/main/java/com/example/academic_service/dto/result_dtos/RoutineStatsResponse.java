package com.example.academic_service.dto.result_dtos;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class RoutineStatsResponse {
    private Integer examRoutineId;
    private String routineTitle;
    private String examTypeName;
    private String className;
    private Integer totalEnrolled;
    private List<SubjectStats> subjectStats;

    @Getter
    @Setter
    public static class SubjectStats {
        private Integer subjectId;
        private String subjectName;
        private boolean fourthSubject;
        private Integer appeared;
        private Integer passed;
        private Integer failed;
        private Double averageMarks;
        private Double highestMarks;
        private Double lowestMarks;
        private Map<String, Integer> gradeDistribution;
    }
}
