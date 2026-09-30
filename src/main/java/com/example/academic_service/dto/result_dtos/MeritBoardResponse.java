package com.example.academic_service.dto.result_dtos;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

/**
 * A class's merit list for one exam or for the year, with every student's class, shift and section
 * position (see MeritBoardService for how they are ranked). The admin panel shows one tab per kind.
 */
@Getter
@Setter
public class MeritBoardResponse {
    private String className;
    private String academicYearName;
    /** The exam (routine title) or "Annual". */
    private String title;
    private String examTypeName;
    private boolean useGpaForResult;
    /** Students are ranked separately per group, so each group gets its own list. */
    private boolean hasGroups;
    /** More than one gender section ("shift") in the class; otherwise there is no shift position. */
    private boolean hasShifts;
    /** At least one student is in a section; otherwise there is no section position. */
    private boolean hasSections;
    /** Passed students by class position (group by group), then failed students by roll. */
    private List<Entry> entries;

    @Getter
    @Setter
    public static class Entry {
        private Long enrollmentId;
        private String studentSystemId;
        private String studentName;
        private Integer classRoll;
        private Integer groupId;
        private String groupName;
        private Integer genderSectionId;
        private String genderSectionName;
        private Long sectionId;
        private String sectionName;
        /** The routine total, or the year's scaled total when the class has one. */
        private BigDecimal totalMarks;
        private Double overallGpa;
        private boolean passed;
        /** All null for a failed student. */
        private Integer classPosition;
        private Integer shiftPosition;
        private Integer sectionPosition;
    }
}
