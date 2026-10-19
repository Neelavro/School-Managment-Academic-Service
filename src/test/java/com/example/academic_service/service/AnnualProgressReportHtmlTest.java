package com.example.academic_service.service;

import com.example.academic_service.dto.result_dtos.AnnualProgressReportData;
import com.example.academic_service.dto.result_dtos.AnnualResultResponse;
import com.example.academic_service.dto.result_dtos.ProgressReportData;
import com.example.academic_service.entity.Grade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Grand Final card HTML from made-up data: two exams with different components, a merged pair,
 * a 4th subject and an absent paper. Set -DannualHtmlOut=<file> to save the page for a visual check.
 */
class AnnualProgressReportHtmlTest {

    @Test
    void cardShowsEachExamsComponentsAndTheAnnualResult() throws Exception {
        AnnualResultResponse annual = new AnnualResultResponse();
        annual.setAcademicYearName("2026");
        annual.setClassName("Class 8");
        annual.setRoutines(List.of(routineInfo(1, "Half Yearly"), routineInfo(2, "Annual Exam")));

        String[] names = {"Quran Majid", "Hadith Sharif", "Arabic 1st Paper", "Arabic 2nd Paper", "Bangla 1st Paper",
                "Bangla 2nd Paper", "English 1st Paper", "English 2nd Paper", "Mathematics", "Science",
                "Bangladesh & Global Studies", "Fiqh", "Aqaid", "ICT", "Agriculture Studies"};
        List<AnnualResultResponse.SubjectInfo> subjects = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            AnnualResultResponse.SubjectInfo si = new AnnualResultResponse.SubjectInfo();
            si.setSubjectId(i + 1);
            si.setSubjectName(names[i]);
            si.setFourthSubject(i == names.length - 1);
            subjects.add(si);
        }
        annual.setSubjects(subjects);

        Map<String, Map<Integer, BigDecimal>> marks = new HashMap<>();
        List<AnnualResultResponse.SubjectResult> results = new ArrayList<>();
        for (AnnualResultResponse.SubjectInfo si : subjects) {
            int id = si.getSubjectId();
            boolean absentInFirst = id == 10;
            AnnualResultResponse.SubjectResult sr = new AnnualResultResponse.SubjectResult();
            sr.setSubjectId(id);
            sr.setMaxMarksRaw(200);
            sr.setAppeared(true);
            sr.setFourthSubject(si.isFourthSubject());
            BigDecimal h1 = absentInFirst ? BigDecimal.ZERO : BigDecimal.valueOf(60 + id);
            BigDecimal h2 = BigDecimal.valueOf(70 + id);
            sr.setMarksRaw(h1.add(h2));
            sr.setGradeName("A");
            sr.setGpaValue(4.0);
            sr.setRoutineBreakdowns(List.of(
                    breakdown(1, h1, !absentInFirst, absentInFirst ? "ABSENT" : null),
                    breakdown(2, h2, true, null)));
            if (!absentInFirst) marks.put("1_" + id, Map.of(11, h1.subtract(BigDecimal.valueOf(20)), 12, BigDecimal.valueOf(20)));
            marks.put("2_" + id, Map.of(11, h2.subtract(BigDecimal.valueOf(25)), 12, BigDecimal.valueOf(15), 13, BigDecimal.TEN));
            results.add(sr);
        }

        AnnualResultResponse.StudentResultRow student = new AnnualResultResponse.StudentResultRow();
        student.setEnrollmentId(7L);
        student.setStudentName("Abdullah Al Mamun");
        student.setStudentSystemId("000123");
        student.setClassRoll(4);
        student.setGenderSectionName("Boys");
        student.setSectionName("A");
        student.setSubjectResults(results);
        student.setOverallGpa(4.25);
        student.setGpaWithout4th(4.0);
        student.setPassed(true);
        AnnualResultResponse.Merit merit = new AnnualResultResponse.Merit();
        merit.setClassRank(3);
        merit.setGenderSectionRank(2);
        merit.setSectionRank(1);
        student.setMerit(merit);
        annual.setStudents(List.of(student));

        AnnualProgressReportData data = new AnnualProgressReportData();
        data.setAnnual(annual);
        data.setRoutines(List.of(
                column(1, "Half Yearly", component(11, "Written"), component(12, "MCQ")),
                column(2, "Annual Exam", component(11, "Written"), component(12, "MCQ"), component(13, "Practical"))));
        data.setMergeGroups(Map.of(3, 1, 4, 1, 5, 2, 6, 2, 7, 3, 8, 3));
        Map<Integer, BigDecimal> highest = new HashMap<>();
        subjects.forEach(s -> highest.put(s.getSubjectId(), BigDecimal.valueOf(190)));
        data.setHighestMarks(highest);
        data.setComponentMarks(Map.of(7L, marks));
        AnnualProgressReportData.StudentDetails d = new AnnualProgressReportData.StudentDetails();
        d.setMotherPhone("01700000000");
        d.setFourthBadgeSubjectIds(Set.of(15));
        data.setDetails(Map.of(7L, d));

        List<Grade> grades = List.of(
                grade("A+", 5.0, 80, 100), grade("A", 4.0, 70, 79), grade("A-", 3.5, 60, 69),
                grade("B", 3.0, 50, 59), grade("C", 2.0, 40, 49), grade("D", 1.0, 33, 39), grade("F", 0.0, 0, 32));

        String html = new ProgressReportPdfService(null, null, null, null)
                .buildAnnualHtml(List.of(data), "Demo Madrasa", "Dhaka", null, "", "", grades);

        assertTrue(html.contains("GRAND FINAL - 2026"));
        assertTrue(html.contains(">Half Yearly</th>") && html.contains(">Annual Exam</th>"));
        assertTrue(html.contains("colspan=\"3\">Half Yearly") && html.contains("colspan=\"4\">Annual Exam"));
        assertTrue(html.contains(">Practical</th>"));
        assertTrue(html.contains(">ABSENT</td>"));
        assertTrue(html.contains("(4<sup>th</sup>)"));
        assertEquals(6, html.split("class=\"merged-grade\"").length - 1, "3 merged pairs, 2 grade cells each");

        String out = System.getProperty("annualHtmlOut");
        if (out != null) Files.writeString(Path.of(out), html);
    }

    private static AnnualResultResponse.RoutineInfo routineInfo(int id, String title) {
        AnnualResultResponse.RoutineInfo ri = new AnnualResultResponse.RoutineInfo();
        ri.setRoutineId(id);
        ri.setRoutineTitle(title);
        return ri;
    }

    private static AnnualResultResponse.RoutineBreakdown breakdown(int routineId, BigDecimal marks, boolean appeared, String status) {
        AnnualResultResponse.RoutineBreakdown rb = new AnnualResultResponse.RoutineBreakdown();
        rb.setRoutineId(routineId);
        rb.setMarksObtained(marks);
        rb.setMaxMarks(100);
        rb.setAppeared(appeared);
        rb.setStatus(status);
        return rb;
    }

    private static ProgressReportData.ComponentInfo component(int id, String name) {
        ProgressReportData.ComponentInfo c = new ProgressReportData.ComponentInfo();
        c.setComponentId(id);
        c.setComponentName(name);
        c.setOrderIndex(id);
        return c;
    }

    private static AnnualProgressReportData.RoutineColumn column(int id, String title, ProgressReportData.ComponentInfo... comps) {
        AnnualProgressReportData.RoutineColumn c = new AnnualProgressReportData.RoutineColumn();
        c.setRoutineId(id);
        c.setTitle(title);
        c.setComponents(List.of(comps));
        return c;
    }

    private static Grade grade(String name, double gpa, double min, double max) {
        Grade g = new Grade();
        g.setName(name);
        g.setGpaValue(gpa);
        g.setMinMark(min);
        g.setMaxMark(max);
        return g;
    }
}
