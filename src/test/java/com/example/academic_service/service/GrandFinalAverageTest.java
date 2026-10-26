package com.example.academic_service.service;

import com.example.academic_service.dto.result_dtos.AnnualProgressReportData;
import com.example.academic_service.dto.result_dtos.ProgressReportData;
import com.example.academic_service.entity.Enrollment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Grand Final row: the average of the counted exams a student sat, ranked on those averages. */
class GrandFinalAverageTest {

    @Test
    void averagesTheExamsSatAndRanksOnTheAverage() {
        // Sample transcript: 899, 900 and 898 (2nd model test not sat) -> 899.
        // Student 2 sat all four with higher totals once added, but a lower average.
        Map<Long, List<ProgressReportData.StudentReport>> sat = Map.of(
                1L, List.of(exam(899, 5.0, true, 0), exam(900, 5.0, true, 0), exam(898, 5.0, true, 0)),
                2L, List.of(exam(880, 5.0, true, 0), exam(890, 5.0, true, 0), exam(885, 5.0, true, 0), exam(870, 5.0, true, 0)),
                3L, List.of(exam(950, 5.0, true, 0), exam(300, 0.0, false, 2)));
        Map<Long, AnnualProgressReportData.GrandFinal> gf = ResultService.buildGrandFinals(
                List.of(enrollment(1L, 1), enrollment(2L, 2), enrollment(3L, 3), enrollment(4L, 4)), sat, true, false);

        AnnualProgressReportData.GrandFinal a = gf.get(1L);
        assertEquals(0, new BigDecimal("899").compareTo(a.getTotalMarks()));
        assertEquals(5.0, a.getGpa());
        assertEquals(3, a.getExamsCounted());
        assertTrue(a.isPassed());
        assertEquals(1, a.getClassRank());

        AnnualProgressReportData.GrandFinal b = gf.get(2L);
        assertEquals(0, new BigDecimal("881.25").compareTo(b.getTotalMarks()));
        assertEquals(2, b.getClassRank(), "ranked on the average, not the sum (3525 > 2697)");

        AnnualProgressReportData.GrandFinal c = gf.get(3L);
        assertFalse(c.isPassed(), "a failed exam fails the Grand Final");
        assertEquals(0.0, c.getGpa());
        assertEquals(2, c.getFailedSubjectCount());
        assertNull(c.getClassRank());

        assertNull(gf.get(4L), "no exam sat: no Grand Final");
    }

    private static ProgressReportData.StudentReport exam(int total, double gpa, boolean passed, int failed) {
        ProgressReportData.StudentReport r = new ProgressReportData.StudentReport();
        r.setTotalMarks(BigDecimal.valueOf(total));
        r.setOverallGpa(gpa);
        r.setGpaWithout4th(gpa);
        r.setPassed(passed);
        r.setFailedSubjectCount(failed);
        return r;
    }

    private static Enrollment enrollment(Long id, int roll) {
        Enrollment e = new Enrollment();
        e.setId(id);
        e.setClassRoll(roll);
        return e;
    }
}
