package com.example.academic_service.service;

import com.example.academic_service.entity.Enrollment;
import com.example.academic_service.entity.ExamAttendance;
import com.example.academic_service.entity.ExamRoutine;
import com.example.academic_service.repository.AttendanceRepository;
import com.example.academic_service.repository.EnrollmentRepository;
import com.example.academic_service.repository.ExamAttendanceRepository;
import com.example.academic_service.repository.ExamRoutineRepository;
import com.example.academic_service.repository.ExamSessionRepository;
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
 * Working days per exam routine and each student's present days, for the progress report (user, 2026-11-02).
 * <p>
 * Present days = working days − the student's absences in the exam's period (the attendance table stores absences
 * only). The period runs from the previous routine's first session up to the day before this routine's first
 * session (the first routine of the year counts every absence before it), per class. A number typed on Progress
 * Report is saved in {@code exam_attendance} and wins over the count; clearing it goes back to the count.
 * The student list uses the same filters as the progress report download.
 */
@Service
@RequiredArgsConstructor
public class ExamAttendanceService {

    private final ExamRoutineRepository examRoutineRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExamAttendanceRepository examAttendanceRepository;
    private final ExamSessionRepository examSessionRepository;
    private final AttendanceRepository attendanceRepository;

    /**
     * {@code absences}: counted from attendance (null when the exam has no session dates for the class);
     * {@code savedPresentDays}: the typed number, null when none; {@code presentDays}: what the PDF prints.
     */
    public record StudentRow(Long enrollmentId, String studentSystemId, String studentName, Integer classRoll,
                             String genderSectionName, String sectionName, String groupName,
                             Integer absences, Integer savedPresentDays, Integer presentDays) {}

    /** {@code countedFrom}/{@code countedTo}: the attendance period, both inclusive (from is null = start of the year). */
    public record Sheet(Integer routineId, String routineTitle, Integer workingDays,
                        LocalDate countedFrom, LocalDate countedTo, List<StudentRow> students) {}

    public record Entry(Long enrollmentId, Integer presentDays) {}

    /** Working and present days for one student (either may be null when unknown). */
    public record Days(Integer working, Integer present) {
        public Integer absent() {
            return working != null && present != null ? Math.max(0, working - present) : null;
        }
    }

    /** The attendance period: {@code from} inclusive (null = no lower bound), {@code to} exclusive. */
    private record Period(LocalDate from, LocalDate to) {}

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
        Integer yearId = yearId(routine);
        List<Enrollment> enrollments = enrollmentRepository.findAllByClassIdAndFilters(
                classId, yearId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll);
        Map<Long, Calc> calc = calc(routine, enrollments);
        Period period = period(routine, classId, new HashMap<>());
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
        return new Sheet(routine.getId(), routine.getTitle(), routine.getWorkingDays(),
                period != null ? period.from() : null, period != null ? period.to().minusDays(1) : null, rows);
    }

    /**
     * Saves the routine's working days (when {@code setWorkingDays}) and the present days typed for the listed students.
     * A null presentDays, or one equal to the counted value, removes the typed number (the count is used).
     * Present days can't exceed the working days.
     */
    @Transactional
    public int save(Integer routineId, boolean setWorkingDays, Integer workingDays, List<Entry> entries) {
        ExamRoutine routine = routine(routineId);
        if (setWorkingDays) {
            if (workingDays != null && (workingDays < 0 || workingDays > 366)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Working days must be between 0 and 366");
            }
            routine.setWorkingDays(workingDays);
            routine.setLastModifiedAt(LocalDateTime.now());
            examRoutineRepository.save(routine);
        }
        Integer limit = routine.getWorkingDays();
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
        Map<Long, Calc> calc = calc(routine, found);

        Map<Long, ExamAttendance> existing = examAttendanceRepository.findByExamRoutineIdAndEnrollmentIdIn(routineId, ids).stream()
                .collect(Collectors.toMap(ExamAttendance::getEnrollmentId, Function.identity()));
        List<ExamAttendance> toSave = new ArrayList<>();
        List<ExamAttendance> toDelete = new ArrayList<>();
        int changed = 0;
        for (Entry e : list) {
            ExamAttendance row = existing.get(e.enrollmentId());
            Integer counted = calc.get(e.enrollmentId()).counted();
            // Only numbers that differ from the count are stored, so later absences still update the rest.
            if (e.presentDays() == null || Objects.equals(e.presentDays(), counted)) {
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
        Map<Long, Integer> out = new HashMap<>();
        calc(routine, enrollmentRepository.findAllById(new HashSet<>(enrollmentIds)))
                .forEach((id, c) -> { if (c.effective() != null) out.put(id, c.effective()); });
        return out;
    }

    /** Days per routine and enrollment, for the Grand Final card: routineId → enrollmentId → days. */
    @Transactional(readOnly = true)
    public Map<Integer, Map<Long, Days>> daysFor(Collection<Integer> routineIds, Collection<Long> enrollmentIds) {
        Map<Integer, Map<Long, Days>> out = new HashMap<>();
        if (routineIds.isEmpty()) return out;
        List<Enrollment> enrollments = enrollmentIds.isEmpty() ? List.of() : enrollmentRepository.findAllById(new HashSet<>(enrollmentIds));
        Map<String, Map<Integer, LocalDate>> firstDates = new HashMap<>();
        for (ExamRoutine r : examRoutineRepository.findAllById(routineIds)) {
            Map<Long, Days> m = new HashMap<>();
            calc(r, enrollments, firstDates).forEach((id, c) -> m.put(id, new Days(r.getWorkingDays(), c.effective())));
            out.put(r.getId(), m);
        }
        return out;
    }

    /** The routine's working days (null when not set). */
    @Transactional(readOnly = true)
    public Integer workingDays(Integer routineId) {
        return examRoutineRepository.findById(routineId).map(ExamRoutine::getWorkingDays).orElse(null);
    }

    // ── Counting ─────────────────────────────────────────────────────────────

    private Map<Long, Calc> calc(ExamRoutine routine, Collection<Enrollment> enrollments) {
        return calc(routine, enrollments, new HashMap<>());
    }

    /** Absences, typed and counted present days for each enrollment; {@code firstDates} caches per year + class. */
    private Map<Long, Calc> calc(ExamRoutine routine, Collection<Enrollment> enrollments,
                                 Map<String, Map<Integer, LocalDate>> firstDates) {
        Map<Long, Calc> out = new HashMap<>();
        if (enrollments.isEmpty()) return out;
        Map<Long, Integer> saved = examAttendanceRepository.findByExamRoutineIdAndEnrollmentIdIn(routine.getId(),
                        enrollments.stream().map(Enrollment::getId).toList()).stream()
                .collect(Collectors.toMap(ExamAttendance::getEnrollmentId, ExamAttendance::getPresentDays, (a, b) -> a));
        Integer working = routine.getWorkingDays();

        // The period depends on the class's exam dates.
        Map<Integer, List<Enrollment>> byClass = enrollments.stream()
                .filter(e -> e.getStudentClass() != null)
                .collect(Collectors.groupingBy(e -> e.getStudentClass().getId()));
        Map<Long, Integer> absences = new HashMap<>();
        for (Map.Entry<Integer, List<Enrollment>> g : byClass.entrySet()) {
            Period p = period(routine, g.getKey(), firstDates);
            if (p == null) continue;
            List<Long> ids = g.getValue().stream().map(Enrollment::getId).toList();
            for (Long id : ids) absences.put(id, 0);
            List<Object[]> rows = p.from() == null
                    ? attendanceRepository.countAbsencesBefore(ids, p.to())
                    : attendanceRepository.countAbsencesBetween(ids, p.from(), p.to());
            for (Object[] r : rows) absences.put(((Number) r[0]).longValue(), ((Number) r[1]).intValue());
        }

        for (Enrollment e : enrollments) {
            Integer a = absences.get(e.getId());
            Integer counted = working != null && a != null ? Math.max(0, working - a) : null;
            out.put(e.getId(), new Calc(a, saved.get(e.getId()), counted));
        }
        return out;
    }

    /**
     * From the previous routine's first session (same year and class) up to this routine's first session (exclusive).
     * Falls back to the routine's start date when the class has no dated session; null when neither exists.
     */
    private Period period(ExamRoutine routine, Integer classId, Map<String, Map<Integer, LocalDate>> cache) {
        Integer yearId = yearId(routine);
        Map<Integer, LocalDate> dates = yearId == null || classId == null ? Map.of()
                : cache.computeIfAbsent(yearId + ":" + classId, k -> firstSessionDates(yearId, classId));
        LocalDate to = dates.getOrDefault(routine.getId(), routine.getRoutineStartDate());
        if (to == null) return null;
        LocalDate from = dates.entrySet().stream()
                .filter(en -> !en.getKey().equals(routine.getId()) && en.getValue().isBefore(to))
                .map(Map.Entry::getValue)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new Period(from, to);
    }

    private Map<Integer, LocalDate> firstSessionDates(Integer yearId, Integer classId) {
        Map<Integer, LocalDate> out = new HashMap<>();
        for (Object[] r : examSessionRepository.findFirstSessionDatesByYearAndClass(yearId, classId)) {
            LocalDate d = r[1] instanceof java.sql.Date sd ? sd.toLocalDate()
                    : r[1] instanceof LocalDate ld ? ld : null;
            if (d != null) out.put(((Number) r[0]).intValue(), d);
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
