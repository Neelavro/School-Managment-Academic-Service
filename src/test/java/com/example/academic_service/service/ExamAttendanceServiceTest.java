package com.example.academic_service.service;

import com.example.academic_service.entity.AcademicYear;
import com.example.academic_service.entity.Enrollment;
import com.example.academic_service.entity.ExamAttendance;
import com.example.academic_service.entity.ExamRoutine;
import com.example.academic_service.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Present days = working days − absences between the previous exam's first session and this exam's first session. */
class ExamAttendanceServiceTest {

    private final ExamRoutineRepository routines = mock(ExamRoutineRepository.class);
    private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
    private final ExamAttendanceRepository saved = mock(ExamAttendanceRepository.class);
    private final ExamSessionRepository sessions = mock(ExamSessionRepository.class);
    private final AttendanceRepository attendance = mock(AttendanceRepository.class);
    private final ExamAttendanceService service =
            new ExamAttendanceService(routines, enrollments, saved, sessions, attendance);

    private final LocalDate firstTerm = LocalDate.of(2026, 3, 1);
    private final LocalDate midterm = LocalDate.of(2026, 7, 1);
    private ExamRoutine first, mid;
    private Enrollment student;

    @BeforeEach
    void setUp() {
        AcademicYear year = new AcademicYear();
        year.setId(1);
        first = routine(10, year, 50);
        mid = routine(11, year, 80);
        com.example.academic_service.entity.Class cls = new com.example.academic_service.entity.Class();
        cls.setId(6);
        student = new Enrollment();
        student.setId(100L);
        student.setStudentClass(cls);
        student.setAcademicYear(year);
        when(enrollments.findAllById(any())).thenReturn(List.of(student));
        when(sessions.findFirstSessionDatesByYearAndClass(1, 6)).thenReturn(List.of(
                new Object[]{10, java.sql.Date.valueOf(firstTerm)},
                new Object[]{11, java.sql.Date.valueOf(midterm)}));
        when(saved.findByExamRoutineIdAndEnrollmentIdIn(anyInt(), any())).thenReturn(List.of());
    }

    @Test
    void firstExamCountsEverythingBeforeIt() {
        when(attendance.countAbsencesBefore(any(), eq(firstTerm))).thenReturn(List.<Object[]>of(new Object[]{100L, 4L}));
        assertEquals(46, service.presentDays(10, List.of(100L)).get(100L));
        verify(attendance, never()).countAbsencesBetween(any(), any(), any());
    }

    @Test
    void laterExamCountsFromThePreviousExamsFirstSession() {
        when(attendance.countAbsencesBetween(any(), eq(firstTerm), eq(midterm))).thenReturn(List.<Object[]>of(new Object[]{100L, 7L}));
        assertEquals(73, service.presentDays(11, List.of(100L)).get(100L));
    }

    @Test
    void noAbsencesMeansPresentEveryWorkingDay() {
        when(attendance.countAbsencesBetween(any(), any(), any())).thenReturn(List.of());
        assertEquals(80, service.presentDays(11, List.of(100L)).get(100L));
    }

    @Test
    void typedNumberWinsOverTheCount() {
        ExamAttendance row = new ExamAttendance();
        row.setExamRoutineId(11);
        row.setEnrollmentId(100L);
        row.setPresentDays(60);
        when(saved.findByExamRoutineIdAndEnrollmentIdIn(eq(11), any())).thenReturn(List.of(row));
        when(attendance.countAbsencesBetween(any(), any(), any())).thenReturn(List.of());
        assertEquals(60, service.presentDays(11, List.of(100L)).get(100L));
    }

    @Test
    void noWorkingDaysMeansBlank() {
        mid.setWorkingDays(null);
        when(attendance.countAbsencesBetween(any(), any(), any())).thenReturn(List.of());
        assertNull(service.presentDays(11, List.of(100L)).get(100L));
    }

    private ExamRoutine routine(int id, AcademicYear year, Integer workingDays) {
        ExamRoutine r = new ExamRoutine();
        r.setId(id);
        r.setAcademicYear(year);
        r.setWorkingDays(workingDays);
        when(routines.findById(id)).thenReturn(Optional.of(r));
        return r;
    }
}
