package com.example.academic_service.dto.result_dtos;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grand Final (annual) progress report for one group of a class. Totals, grades,
 * GPA, pass/fail and positions come from {@link AnnualResultResponse} unchanged,
 * so the report matches the Annual Results screen; this adds what the card needs
 * on top: each exam's component marks, highest marks and the student's details.
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
