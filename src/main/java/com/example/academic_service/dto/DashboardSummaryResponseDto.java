package com.example.academic_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
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
        /** The same students counted by their own gender (Male / Female; genderId null = not set). */
        private List<StudentGenderCount> studentGenders = new ArrayList<>();

        public GenderBucket(Integer genderSectionId, String genderName, long count) {
            this.genderSectionId = genderSectionId;
            this.genderName = genderName;
            this.count = count;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentGenderCount {
        private Integer genderId;
        private String gender;
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
