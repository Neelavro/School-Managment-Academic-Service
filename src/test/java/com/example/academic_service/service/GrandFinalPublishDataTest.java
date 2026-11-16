package com.example.academic_service.service;

import com.example.academic_service.dto.result_dtos.AnnualProgressReportData;
import com.example.academic_service.entity.GradingPolicy;
import com.example.academic_service.entity.SystemSettings;
import com.example.academic_service.repository.GradingPolicyRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * What the Grand Final publish stores per student (ProgressReportPdfService.grandFinalForPublish), from the same
 * made-up class as AnnualProgressReportHtmlTest: the portal's result and the transcript page with placeholders.
 */
class GrandFinalPublishDataTest {

    @Test
    @SuppressWarnings("unchecked")
    void storesThePortalResultAndTheTranscriptPage() throws Exception {
        AnnualProgressReportData data = AnnualProgressReportHtmlTest.sampleData();
        ResultService results = mock(ResultService.class);
        when(results.getAnnualProgressReportDataPartitioned(eq(5), eq(9), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(data));
        SystemSettingsService settings = mock(SystemSettingsService.class);
        SystemSettings s = new SystemSettings();
        s.setInstitutionName("Demo Madrasa");
        s.setAddress("Dhaka");
        when(settings.getSettings()).thenReturn(s);
        GradingPolicyRepository policies = mock(GradingPolicyRepository.class);
        GradingPolicy policy = new GradingPolicy();
        policy.setGrades(AnnualProgressReportHtmlTest.sampleGrades());
        when(policies.findByIsActive(true)).thenReturn(List.of(policy));
        ExamAttendanceService attendance = mock(ExamAttendanceService.class);
        when(attendance.daysFor(anyList(), anyList())).thenReturn(Map.of());

        List<ProgressReportPdfService.GrandFinalStudent> stored =
                new ProgressReportPdfService(results, settings, policies, null, attendance).grandFinalForPublish(5, 9);

        assertEquals(1, stored.size());
        ProgressReportPdfService.GrandFinalStudent g = stored.get(0);
        assertEquals(7L, g.enrollmentId());
        assertEquals("000123", g.studentSystemId());
        assertFalse(g.passed());
        assertEquals(0.0, g.gpa());

        // The page is the transcript card, with the images left for print time.
        assertTrue(g.pageHtml().contains("GRAND FINAL - 2026"));
        assertTrue(g.pageHtml().contains("__EDUNIX_PHOTO__") && g.pageHtml().contains("__EDUNIX_LOGO__"));
        assertFalse(g.pageHtml().contains("<html"), "one page, wrapped at print time");

        // The portal result: the Grand Final row on top, subjects with each exam's marks, each exam's row.
        Map<String, Object> r = (Map<String, Object>) JsonMapper.builder().build()
                .readValue(JsonMapper.builder().build().writeValueAsString(g.result()), Map.class);
        assertEquals("000123", r.get("studentSystemId"));
        assertEquals(1020.0, ((Number) r.get("totalMarks")).doubleValue());
        assertEquals(false, r.get("passed"));
        assertNull(r.get("positions"), "a failed Grand Final has no positions");
        assertEquals(4, ((List<?>) r.get("routines")).size());
        List<Map<String, Object>> subjects = (List<Map<String, Object>>) r.get("subjectResults");
        assertEquals(15, subjects.size());
        Map<String, Object> science = subjects.get(9);
        assertEquals("Science", science.get("subjectName"));
        List<Map<String, Object>> exams = (List<Map<String, Object>>) science.get("exams");
        assertEquals("Half Yearly", exams.get(0).get("title"));
        assertEquals("ABSENT", exams.get(0).get("status"));
        List<Map<String, Object>> examRows = (List<Map<String, Object>>) r.get("exams");
        assertEquals(4, examRows.size());
        assertEquals(true, examRows.get(1).get("passed"));
        assertEquals(2, ((Number) examRows.get(1).get("classPosition")).intValue());
        assertEquals(false, examRows.get(3).get("sat"), "no summary for the 2nd Model Test");
    }
}
