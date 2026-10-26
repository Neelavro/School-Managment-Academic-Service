package com.example.academic_service.dto.result_dtos;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grand Final (annual) progress report for one group of a class. The subject rows come from
 * {@link AnnualResultResponse}; the Grand Final row is its own: the average of the counted exams
 * the student sat, with positions ranked on those averages (user, 2026-10-26). The card adds each
 * exam's component marks and the student's details on top.
 */
@Getter
@Setter
public class AnnualProgressReportData {

    private AnnualResultResponse annual;

    /** The exams counted for the annual result, in routine order, each with the components its papers use. */
    private List<RoutineColumn> routines;

    /** subjectId -> merge group id (subjects printed as one combined grade). */
    private Map<Integer, Integer> mergeGroups;

    /** subjectId -> highest annual total (all exams added) in the class or group. */
    private Map<Integer, BigDecimal> highestMarks;

    /** enrollmentId -> "routineId_subjectId" -> componentId -> marks obtained. */
    private Map<Long, Map<String, Map<Integer, BigDecimal>>> componentMarks;

    /** enrollmentId -> photo, phones and the subject that gets the (4th) badge. */
    private Map<Long, StudentDetails> details;

    /**
     * routineId -> enrollmentId -> that exam's result (total, GPA, GPA without 4th, failed subjects,
     * pass/fail, positions) — the per-exam progress report's numbers, without the subject rows.
     * An exam the group has no papers in is missing.
     */
    private Map<Integer, Map<Long, ProgressReportData.StudentReport>> examSummaries;

    /** enrollmentId -> Grand Final row. Missing when the student sat none of the counted exams. */
    private Map<Long, GrandFinal> grandFinals;

    /**
     * The Grand Final row: total, GPA and GPA without 4th are the averages over the counted exams
     * (consider_for_annual_result) the student sat. A fail in any of them fails the Grand Final
     * (GPA 0, no position). Failed subjects add up across those exams.
     */
    @Getter
    @Setter
    public static class GrandFinal {
        private int examsCounted;
        private BigDecimal totalMarks;
        private Double gpa;
        private Double gpaWithout4th;
        private boolean passed;
        private int failedSubjectCount;
        private Integer classRank;
        private Integer genderSectionRank;
        private Integer sectionRank;
    }

    @Getter
    @Setter
    public static class RoutineColumn {
        private Integer routineId;
        private String title;
        private List<ProgressReportData.ComponentInfo> components;
    }

    @Getter
    @Setter
    public static class StudentDetails {
        private String fatherPhone;
        private String motherPhone;
        private String imageUrl;
        private Set<Integer> fourthBadgeSubjectIds;
    }
}
