package com.example.academic_service.service;

import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Sub-subject marks add up into the parent (Arts = Drawing + Singing). */
class SubSubjectsTest {

    private static final int ARTS = 10, DRAWING = 11, SINGING = 12, MATH = 20;
    private static final Map<Integer, Integer> PARENT_OF = Map.of(DRAWING, ARTS, SINGING, ARTS);

    private static ExamComponent component(int id) {
        ExamComponent c = new ExamComponent();
        c.setId(id);
        c.setName("C" + id);
        c.setOrderIndex(id);
        return c;
    }

    private static final ExamComponent WRITTEN = component(1), PRACTICAL = component(2);

    private static StudentMark mark(long enrollment, int subject, ExamComponent c, String marks, String status) {
        StudentMark m = new StudentMark();
        m.setEnrollmentId(enrollment);
        m.setRoutineId(5);
        m.setSubjectId(subject);
        m.setExamComponent(c);
        m.setMarksObtained(marks != null ? new BigDecimal(marks) : null);
        m.setStatus(status);
        return m;
    }

    private static Map<Integer, BigDecimal> parentMarks(List<StudentMark> out, long enrollment) {
        Map<Integer, BigDecimal> map = new HashMap<>();
        out.stream().filter(m -> m.getEnrollmentId() == enrollment && m.getSubjectId() == ARTS && m.getMarksObtained() != null)
                .forEach(m -> map.put(m.getExamComponent().getId(), m.getMarksObtained()));
        return map;
    }

    @Test
    void marksAddUpPerComponent() {
        List<StudentMark> out = SubSubjects.rollUp(List.of(
                mark(1, DRAWING, WRITTEN, "15", null),
                mark(1, DRAWING, PRACTICAL, "8", null),
                mark(1, SINGING, PRACTICAL, "20.5", null),
                mark(1, MATH, WRITTEN, "70", null)), PARENT_OF);

        assertEquals(Map.of(1, new BigDecimal("15"), 2, new BigDecimal("28.5")), parentMarks(out, 1));
        assertTrue(out.stream().noneMatch(m -> m.getSubjectId() == DRAWING || m.getSubjectId() == SINGING));
        assertTrue(out.stream().anyMatch(m -> m.getSubjectId() == MATH && m.getMarksObtained().intValue() == 70));
    }

    @Test
    void absentInOnePartStillCountsTheOthers() {
        List<StudentMark> out = SubSubjects.rollUp(List.of(
                mark(1, DRAWING, WRITTEN, null, "ABSENT"),
                mark(1, SINGING, PRACTICAL, "20", null)), PARENT_OF);
        assertEquals(Map.of(2, new BigDecimal("20")), parentMarks(out, 1));
        assertTrue(out.stream().noneMatch(m -> "ABSENT".equals(m.getStatus())));
    }

    @Test
    void absentInEveryPartIsAbsent() {
        List<StudentMark> out = SubSubjects.rollUp(List.of(
                mark(1, DRAWING, WRITTEN, null, "ABSENT"),
                mark(1, SINGING, PRACTICAL, null, "ABSENT")), PARENT_OF);
        assertEquals(1, out.size());
        assertEquals("ABSENT", out.get(0).getStatus());
        assertEquals(ARTS, out.get(0).getSubjectId());
    }

    @Test
    void expelledInAnyPartIsExpelled() {
        List<StudentMark> out = SubSubjects.rollUp(List.of(
                mark(1, DRAWING, WRITTEN, null, "EXPELLED"),
                mark(1, SINGING, PRACTICAL, "20", null)), PARENT_OF);
        assertEquals(1, out.size());
        assertEquals("EXPELLED", out.get(0).getStatus());
    }

    @Test
    void parentsOwnOldMarksAreIgnored() {
        List<StudentMark> out = SubSubjects.rollUp(List.of(
                mark(1, ARTS, WRITTEN, "90", null),
                mark(1, DRAWING, WRITTEN, "10", null)), PARENT_OF);
        assertEquals(Map.of(1, new BigDecimal("10")), parentMarks(out, 1));
    }

    @Test
    void studentsStaySeparate() {
        List<StudentMark> out = SubSubjects.rollUp(List.of(
                mark(1, DRAWING, WRITTEN, "10", null),
                mark(2, DRAWING, WRITTEN, "12", null),
                mark(2, SINGING, WRITTEN, "3", null)), PARENT_OF);
        assertEquals(Map.of(1, new BigDecimal("10")), parentMarks(out, 1));
        assertEquals(Map.of(1, new BigDecimal("15")), parentMarks(out, 2));
    }

    @Test
    void mergedComponentsAddMaxMarksAndDropPassMarks() {
        SubjectRepository subjects = mock(SubjectRepository.class);
        MarkingStructureRepository structures = mock(MarkingStructureRepository.class);
        MarkingStructureComponentRepository components = mock(MarkingStructureComponentRepository.class);
        SubSubjects sub = new SubSubjects(subjects, structures, components);

        ExamType exam = new ExamType();
        exam.setId(3);
        com.example.academic_service.entity.Class cls = new com.example.academic_service.entity.Class();
        cls.setId(7);
        MarkingStructure parent = structure(100, exam, cls, ARTS, 100);
        MarkingStructure drawing = structure(101, exam, cls, DRAWING, 50);
        MarkingStructure singing = structure(102, exam, cls, SINGING, 50);
        when(structures.findClassWideAndDeletedAtIsNull(3, 7, DRAWING)).thenReturn(List.of(drawing));
        when(structures.findClassWideAndDeletedAtIsNull(3, 7, SINGING)).thenReturn(List.of(singing));
        when(components.findAllByMarkingStructureInAndDeletedAtIsNull(anyList())).thenReturn(List.of(
                msc(drawing, WRITTEN, 30, 10), msc(drawing, PRACTICAL, 20, 7), msc(singing, PRACTICAL, 50, 17)));

        List<MarkingStructureComponent> merged = sub.mergedComponents(parent, List.of(DRAWING, SINGING), null);
        assertEquals(2, merged.size());
        assertEquals(1, merged.get(0).getExamComponent().getId());
        assertEquals(30, merged.get(0).getMaxMarks());
        assertEquals(70, merged.get(1).getMaxMarks());
        assertNull(merged.get(1).getPassMarks());
        assertSame(parent, merged.get(0).getMarkingStructure());
    }

    private static MarkingStructure structure(int id, ExamType exam, com.example.academic_service.entity.Class cls, int subjectId, int total) {
        Subject s = new Subject();
        s.setId(subjectId);
        MarkingStructure m = new MarkingStructure();
        m.setId(id);
        m.setExamType(exam);
        m.setExamClass(cls);
        m.setSubject(s);
        m.setTotalMarks(total);
        return m;
    }

    private static MarkingStructureComponent msc(MarkingStructure s, ExamComponent c, int max, Integer pass) {
        MarkingStructureComponent m = new MarkingStructureComponent();
        m.setMarkingStructure(s);
        m.setExamComponent(c);
        m.setMaxMarks(max);
        m.setPassMarks(pass);
        return m;
    }
}
