package com.example.academic_service.dto.result_dtos;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class SessionStatsResponse {
    private Integer examSessionId;
    private String className;
    private String subjectName;
    private String examTypeName;
    private Integer totalEnrolled;
    private Integer appeared;
    private Integer passed;
    private Integer failed;
    private Double averageMarks;
    private Double highestMarks;
    private Double lowestMarks;
    private Map<String, Integer> gradeDistribution;
}
