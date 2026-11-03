package com.example.academic_service.service;

import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Exam Days decide the period: from the previous exam's Exam Days start (or the year start) to the day before this
 * exam's. Working days = class days in it (no weekends, no holidays); present days = working days − absences.
 */
class ExamAttendanceServiceTest {

    private final ExamRoutineRepository routines = mock(ExamRoutineRepository.class);
    private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
    private final ExamAttendanceRepository saved = mock(ExamAttendanceRepository.class);
    private final AttendanceRepository attendance = mock(AttendanceRepository.class);
    private final AcademicCalendarEntryRepository calendar = mock(AcademicCalendarEntryRepository.class);
    private final SystemSettingsService settings = mock(SystemSettingsService.class);
    private final SchoolCalendarService schoolCalendar = new SchoolCalendarService(settings, mock(SystemSettingsRepository.class), calendar);
    private final ExamAttendanceService service =
            new ExamAttendanceService(routines, enrollments, saved, attendance, calendar, schoolCalendar);

    // 2026: Model Test exam days start Mon 2 Mar, Half Yearly Mon 3 Aug. Weekends Friday + Saturday.
    private final LocalDate modelStart = LocalDate.of(2026, 3, 2);
    private final LocalDate halfStart = LocalDate.of(2026, 8, 3);
    private ExamRoutine model, half;
    private AcademicYear year;
    private final List<AcademicCalendarEntry> holidays = new ArrayList<>();

    @BeforeEach
    void setUp() {
        year = new AcademicYear();
        year.setId(1);
        year.setYearName("2026");
        model = routine(10, null);
        half = routine(11, null);
        Enrollment student = new Enrollment();
        student.setId(100L);
        student.setAcademicYear(year);
        when(enrollments.findAllById(any())).thenReturn(List.of(student));
        when(calendar.findByAcademicYear_IdAndTypeAndIsActiveTrueOrderByStartDateAsc(1, CalendarEntryType.EXAM_DAY))
                .thenReturn(List.of(examDays(10, modelStart), examDays(11, halfStart)));
        when(calendar.findByAcademicYear_IdAndTypeAndIsActiveTrueOrderByStartDateAsc(1, CalendarEntryType.HOLIDAY)).thenReturn(holidays);
        when(saved.findByExamRoutineIdAndEnrollmentIdIn(anyInt(), any())).thenReturn(List.of());
        SystemSettings s = new SystemSettings();
        s.setWeekendDays("FRIDAY,SATURDAY");
        when(settings.getSettings()).thenReturn(s);
    }

    @Test
    void firstExamCountsFromTheYearStart() {
        // 1 Jan – 1 Mar 2026: 60 days, 18 Fridays + Saturdays → 42 class days.
        when(attendance.countAbsencesBetween(any(), eq(LocalDate.of(2026, 1, 1)), eq(modelStart))).thenReturn(List.<Object[]>of(new Object[]{100L, 3L}));
        assertEquals(42, service.workingDays(10));
        assertEquals(39, service.presentDays(10, List.of(100L)).get(100L));
    }

    @Test
    void yearStartDateIsUsedWhenSet() {
        year.setStartDate(LocalDate.of(2026, 2, 1));
        when(attendance.countAbsencesBetween(any(), eq(LocalDate.of(2026, 2, 1)), eq(modelStart))).thenReturn(List.of());
        // 1 Feb – 1 Mar: 29 days, 8 weekend days → 21.
        assertEquals(21, service.workingDays(10));
    }

    @Test
    void laterExamCountsFromThePreviousExamDays() {
        when(attendance.countAbsencesBetween(any(), eq(modelStart), eq(halfStart))).thenReturn(List.<Object[]>of(new Object[]{100L, 7L}));
        int working = service.workingDays(11);
        assertEquals(working - 7, service.presentDays(11, List.of(100L)).get(100L));
    }

    @Test
    void holidaysAreNotWorkingDays() {
        int before = service.workingDays(10);
        AcademicCalendarEntry h = new AcademicCalendarEntry();
        h.setType(CalendarEntryType.HOLIDAY);
        h.setStartDate(LocalDate.of(2026, 2, 16)); // Mon–Wed
        h.setEndDate(LocalDate.of(2026, 2, 18));
        holidays.add(h);
        assertEquals(before - 3, service.workingDays(10));
    }

    @Test
    void typedWorkingDaysWin() {
        model.setWorkingDays(50);
        when(attendance.countAbsencesBetween(any(), any(), any())).thenReturn(List.of());
        assertEquals(50, service.workingDays(10));
        assertEquals(50, service.presentDays(10, List.of(100L)).get(100L));
    }

    @Test
    void typedPresentDaysWin() {
        ExamAttendance row = new ExamAttendance();
        row.setExamRoutineId(11);
        row.setEnrollmentId(100L);
        row.setPresentDays(60);
        when(saved.findByExamRoutineIdAndEnrollmentIdIn(eq(11), any())).thenReturn(List.of(row));
        when(attendance.countAbsencesBetween(any(), any(), any())).thenReturn(List.of());
        assertEquals(60, service.presentDays(11, List.of(100L)).get(100L));
    }

    @Test
    void noExamDaysMeansNothingCounted() {
        ExamRoutine test = routine(12, null);
        assertNull(service.workingDays(12));
        assertTrue(service.presentDays(12, List.of(100L)).isEmpty());
        verifyNoInteractions(attendance);
        assertNotNull(test);
    }

    @Test
    void defaultWeekendIsFriday() {
        SystemSettings s = new SystemSettings();
        when(settings.getSettings()).thenReturn(s);
        assertEquals(List.of(DayOfWeek.FRIDAY), schoolCalendar.weekends());
    }

    private ExamRoutine routine(int id, Integer workingDays) {
        ExamRoutine r = new ExamRoutine();
        r.setId(id);
        r.setAcademicYear(year);
        r.setWorkingDays(workingDays);
        when(routines.findById(id)).thenReturn(Optional.of(r));
        return r;
    }

    private AcademicCalendarEntry examDays(int routineId, LocalDate start) {
        AcademicCalendarEntry e = new AcademicCalendarEntry();
        e.setType(CalendarEntryType.EXAM_DAY);
        e.setExamRoutineId(routineId);
        e.setStartDate(start);
        e.setEndDate(start.plusDays(14));
        return e;
    }
}
