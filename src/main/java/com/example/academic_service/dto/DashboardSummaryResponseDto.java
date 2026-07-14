package com.example.academic_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponseDto {

    private Instant generatedAt;
    private String academicYearName;
    private long totalStudents;
    private List<GenderBucket> overall;
    private List<ClassBreakdown> byClass;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenderBucket {
        private Integer genderSectionId;
        private String genderName;
        private long count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassBreakdown {
        private Integer classId;
        private String className;
        private long total;
        private List<GenderBucket> buckets;
    }
}
