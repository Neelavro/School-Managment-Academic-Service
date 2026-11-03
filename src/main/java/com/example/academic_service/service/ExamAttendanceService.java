package com.example.academic_service.service;

import com.example.academic_service.entity.AcademicCalendarEntry;
import com.example.academic_service.entity.CalendarEntryType;
import com.example.academic_service.entity.Enrollment;
import com.example.academic_service.entity.ExamAttendance;
import com.example.academic_service.entity.ExamRoutine;
import com.example.academic_service.repository.AcademicCalendarEntryRepository;
import com.example.academic_service.repository.AttendanceRepository;
import com.example.academic_service.repository.EnrollmentRepository;
import com.example.academic_service.repository.ExamAttendanceRepository;
import com.example.academic_service.repository.ExamRoutineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Working days per exam routine and each student's present days, for the progress report (user, 2026-11-02/03).
 * <p>
 * The exam's period comes from Academic Calendar → Exam Days: from the start of the previous exam's Exam Days (or the
 * academic year's start for the first exam) up to the day before this exam's Exam Days begin.
 * <ul>
 *   <li>Working days = the class days in that period (not a weekend, not a Holiday). A number typed for the routine
 *       ({@code exam_routine.working_days}) wins; one equal to the count is not stored.</li>
 *   <li>Present days = working days − the student's absences in the period (the attendance table stores absences
 *       only). A number typed for the student ({@code exam_attendance}) wins; clearing it goes back to the count.</li>
 * </ul>
 * An exam without Exam Days has no period: nothing is counted, typed numbers still print.
 */
@Service
@RequiredArgsConstructor
public class ExamAttendanceService {

    private final ExamRoutineRepository examRoutineRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExamAttendanceRepository examAttendanceRepository;
    private final AttendanceRepository attendanceRepository;
    private final AcademicCalendarEntryRepository calendarRepository;
    private final SchoolCalendarService schoolCalendarService;

    /**
     * {@code absences}: counted from attendance (null without a period); {@code savedPresentDays}: the typed number,
     * null when none; {@code presentDays}: what the PDF prints.
     */
    public record StudentRow(Long enrollmentId, String studentSystemId, String studentName, Integer classRoll,
                             String genderSectionName, String sectionName, String groupName,
                             Integer absences, Integer savedPresentDays, Integer presentDays) {}

    /**
     * {@code workingDays}: what the PDF prints (typed, else counted); {@code savedWorkingDays}: the typed number or
     * null; {@code countedWorkingDays}: from the calendar. {@code countedFrom}/{@code countedTo}: the period, both
     * inclusive (from null = no academic year start). {@code examDaysMissing}: the exam has no Exam Days.
     */
    public record Sheet(Integer routineId, String routineTitle, Integer workingDays, Integer savedWorkingDays,
                        Integer countedWorkingDays, LocalDate countedFrom, LocalDate countedTo, boolean examDaysMissing,
                        List<StudentRow> students) {}

    public record Entry(Long enrollmentId, Integer presentDays) {}

    /** Working and present days for one student (either may be null when unknown). */
    public record Days(Integer working, Integer present) {
        public Integer absent() {
            return working != null && present != null ? Math.max(0, working - present) : null;
        }
    }

    /** The exam's period: {@code from} inclusive (null = unknown year start), {@code to} exclusive. */
    record Period(LocalDate from, LocalDate to) {}

    /** One student's numbers for one routine. */
    private record Calc(Integer absences, Integer saved, Integer counted) {
        Integer effective() {
            return saved != null ? saved : counted;
        }
    }

    @Transactional(readOnly = true)
    public Sheet sheet(Integer routineId, Integer classId, Integer shiftId, Integer genderSectionId, Long sectionId,
                       Integer groupId, Integer startRoll, Integer endRoll) {
        ExamRoutine routine = routine(routineId);
        List<Enrollment> enrollments = enrollmentRepository.findAllByClassIdAndFilters(
                classId, yearId(routine), shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll);
        Period period = period(routine);
        Integer counted = countedWorkingDays(routine, period);
        Map<Long, Calc> calc = calc(routine, period, effectiveWorking(routine, counted), enrollments);
        List<StudentRow> rows = enrollments.stream()
                .sorted(Comparator.comparing(Enrollment::getClassRoll, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(e -> nvl(e.getStudentSystemId())))
                .map(e -> {
                    Calc c = calc.get(e.getId());
                    return new StudentRow(
                            e.getId(),
                            e.getStudentSystemId(),
                            e.getStudent() != null ? nvl(e.getStudent().getNameEnglish()) : "",
                            e.getClassRoll(),
                            e.getGenderSection() != null ? e.getGenderSection().getGenderName() : null,
                            e.getSection() != null ? e.getSection().getSectionName() : null,
                            e.getStudentGroup() != null ? e.getStudentGroup().getGroupName() : null,
                            c.absences(), c.saved(), c.effective());
                })
                .toList();
        return new Sheet(routine.getId(), routine.getTitle(), effectiveWorking(routine, counted), routine.getWorkingDays(), counted,
                period != null ? period.from() : null, period != null ? period.to().minusDays(1) : null, period == null, rows);
    }

    /**
     * Saves the routine's working days (when {@code setWorkingDays}; null or the counted number = use the count) and
     * the present days typed for the listed students (null or the counted number = use the count). Present days
     * can't exceed the working days.
     */
    @Transactional
    public int save(Integer routineId, boolean setWorkingDays, Integer workingDays, List<Entry> entries) {
        ExamRoutine routine = routine(routineId);
        Period period = period(routine);
        Integer counted = countedWorkingDays(routine, period);
        if (setWorkingDays) {
            if (workingDays != null && (workingDays < 0 || workingDays > 366)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Working days must be between 0 and 366");
            }
            routine.setWorkingDays(Objects.equals(workingDays, counted) ? null : workingDays);
            routine.setLastModifiedAt(LocalDateTime.now());
            examRoutineRepository.save(routine);
        }
        Integer limit = effectiveWorking(routine, counted);
        List<Entry> list = entries != null ? entries.stream().filter(e -> e != null && e.enrollmentId() != null).toList() : List.of();
        for (Entry e : list) {
            Integer p = e.presentDays();
            if (p != null && p < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Present days cannot be negative");
            if (p != null && limit != null && p > limit) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Present days (" + p + ") cannot be more than the working days (" + limit + ")");
            }
        }
        if (list.isEmpty()) return 0;

        Set<Long> ids = list.stream().map(Entry::enrollmentId).collect(Collectors.toSet());
        // Only students enrolled in the routine's academic year.
        Integer yearId = yearId(routine);
        List<Enrollment> found = enrollmentRepository.findAllById(ids);
        Map<Long, Enrollment> enrollments = found.stream().collect(Collectors.toMap(Enrollment::getId, Function.identity()));
        for (Long id : ids) {
            Enrollment en = enrollments.get(id);
            if (en == null || en.getAcademicYear() == null || !Objects.equals(en.getAcademicYear().getId(), yearId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A student in the list is not in this exam's academic year");
            }
        }
        Map<Long, Calc> calc = calc(routine, period, limit, found);

        Map<Long, ExamAttendance> existing = examAttendanceRepository.findByExamRoutineIdAndEnrollmentIdIn(routineId, ids).stream()
                .collect(Collectors.toMap(ExamAttendance::getEnrollmentId, Function.identity()));
        List<ExamAttendance> toSave = new ArrayList<>();
        List<ExamAttendance> toDelete = new ArrayList<>();
        int changed = 0;
        for (Entry e : list) {
            ExamAttendance row = existing.get(e.enrollmentId());
            Integer countedPresent = calc.get(e.enrollmentId()).counted();
            // Only numbers that differ from the count are stored, so later absences still update the rest.
            if (e.presentDays() == null || Objects.equals(e.presentDays(), countedPresent)) {
                if (row != null) {
                    toDelete.add(row);
                    changed++;
                }
                continue;
            }
            if (row != null && Objects.equals(row.getPresentDays(), e.presentDays())) continue;
            if (row == null) {
                row = new ExamAttendance();
                row.setExamRoutineId(routineId);
                row.setEnrollmentId(e.enrollmentId());
            }
            row.setPresentDays(e.presentDays());
            row.setUpdatedAt(LocalDateTime.now());
            toSave.add(row);
            changed++;
        }
        examAttendanceRepository.deleteAll(toDelete);
        examAttendanceRepository.saveAll(toSave);
        return changed;
    }

    /** Present days the PDF prints (typed, else counted) by enrollment for one routine; missing = unknown. */
    @Transactional(readOnly = true)
    public Map<Long, Integer> presentDays(Integer routineId, Collection<Long> enrollmentIds) {
        if (enrollmentIds.isEmpty()) return Map.of();
        ExamRoutine routine = routine(routineId);
        Period period = period(routine);
        Integer working = effectiveWorking(routine, countedWorkingDays(routine, period));
        Map<Long, Integer> out = new HashMap<>();
        calc(routine, period, working, enrollmentRepository.findAllById(new HashSet<>(enrollmentIds)))
                .forEach((id, c) -> { if (c.effective() != null) out.put(id, c.effective()); });
        return out;
    }

    /** Days per routine and enrollment, for the Grand Final card: routineId → enrollmentId → days. */
    @Transactional(readOnly = true)
    public Map<Integer, Map<Long, Days>> daysFor(Collection<Integer> routineIds, Collection<Long> enrollmentIds) {
        Map<Integer, Map<Long, Days>> out = new HashMap<>();
        if (routineIds.isEmpty()) return out;
        List<Enrollment> enrollments = enrollmentIds.isEmpty() ? List.of() : enrollmentRepository.findAllById(new HashSet<>(enrollmentIds));
        for (ExamRoutine r : examRoutineRepository.findAllById(routineIds)) {
            Period period = period(r);
            Integer working = effectiveWorking(r, countedWorkingDays(r, period));
            Map<Long, Days> m = new HashMap<>();
            calc(r, period, working, enrollments).forEach((id, c) -> m.put(id, new Days(working, c.effective())));
            out.put(r.getId(), m);
        }
        return out;
    }

    /** The working days the PDF prints for a routine (typed, else counted; null when neither). */
    @Transactional(readOnly = true)
    public Integer workingDays(Integer routineId) {
        ExamRoutine routine = routine(routineId);
        return effectiveWorking(routine, countedWorkingDays(routine, period(routine)));
    }

    // ── Counting ─────────────────────────────────────────────────────────────

    /**
     * From the start of the previous exam's Exam Days (or the academic year's start) up to this exam's Exam Days
     * start (exclusive). Null when this exam has no Exam Days.
     */
    Period period(ExamRoutine routine) {
        Integer yearId = yearId(routine);
        if (yearId == null) return null;
        List<AcademicCalendarEntry> examDays = calendarRepository
                .findByAcademicYear_IdAndTypeAndIsActiveTrueOrderByStartDateAsc(yearId, CalendarEntryType.EXAM_DAY);
        LocalDate to = examDays.stream()
                .filter(e -> Objects.equals(e.getExamRoutineId(), routine.getId()))
                .map(AcademicCalendarEntry::getStartDate)
                .min(Comparator.naturalOrder())
                .orElse(null);
        if (to == null) return null;
        LocalDate from = examDays.stream()
                .filter(e -> e.getExamRoutineId() != null && !e.getExamRoutineId().equals(routine.getId()) && e.getStartDate().isBefore(to))
                .map(AcademicCalendarEntry::getStartDate)
                .max(Comparator.naturalOrder())
                .orElse(SchoolCalendarService.yearStart(routine.getAcademicYear()));
        return new Period(from, to);
    }

    private Integer countedWorkingDays(ExamRoutine routine, Period period) {
        return period == null ? null : schoolCalendarService.classDays(yearId(routine), period.from(), period.to());
    }

    private static Integer effectiveWorking(ExamRoutine routine, Integer counted) {
        return routine.getWorkingDays() != null ? routine.getWorkingDays() : counted;
    }

    /** Absences, typed and counted present days for each enrollment. */
    private Map<Long, Calc> calc(ExamRoutine routine, Period period, Integer working, Collection<Enrollment> enrollments) {
        Map<Long, Calc> out = new HashMap<>();
        if (enrollments.isEmpty()) return out;
        List<Long> ids = enrollments.stream().map(Enrollment::getId).toList();
        Map<Long, Integer> saved = examAttendanceRepository.findByExamRoutineIdAndEnrollmentIdIn(routine.getId(), ids).stream()
                .collect(Collectors.toMap(ExamAttendance::getEnrollmentId, ExamAttendance::getPresentDays, (a, b) -> a));
        Map<Long, Integer> absences = new HashMap<>();
        if (period != null) {
            for (Long id : ids) absences.put(id, 0);
            List<Object[]> rows = period.from() == null
                    ? attendanceRepository.countAbsencesBefore(ids, period.to())
                    : attendanceRepository.countAbsencesBetween(ids, period.from(), period.to());
            for (Object[] r : rows) absences.put(((Number) r[0]).longValue(), ((Number) r[1]).intValue());
        }
        for (Long id : ids) {
            Integer a = absences.get(id);
            Integer counted = working != null && a != null ? Math.max(0, working - a) : null;
            out.put(id, new Calc(a, saved.get(id), counted));
        }
        return out;
    }

    private ExamRoutine routine(Integer id) {
        return examRoutineRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam routine not found"));
    }

    private static Integer yearId(ExamRoutine routine) {
        return routine.getAcademicYear() != null ? routine.getAcademicYear().getId() : null;
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
