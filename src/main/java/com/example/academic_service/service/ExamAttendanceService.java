package com.example.academic_service.service;

import com.example.academic_service.entity.Enrollment;
import com.example.academic_service.entity.ExamAttendance;
import com.example.academic_service.entity.ExamRoutine;
import com.example.academic_service.repository.EnrollmentRepository;
import com.example.academic_service.repository.ExamAttendanceRepository;
import com.example.academic_service.repository.ExamRoutineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Working days per exam routine and each student's present days, for the progress report (user, 2026-11-02).
 * The student list uses the same filters as the progress report download.
 */
@Service
@RequiredArgsConstructor
public class ExamAttendanceService {

    private final ExamRoutineRepository examRoutineRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExamAttendanceRepository examAttendanceRepository;

    public record StudentRow(Long enrollmentId, String studentSystemId, String studentName, Integer classRoll,
                             String genderSectionName, String sectionName, String groupName, Integer presentDays) {}

    public record Sheet(Integer routineId, String routineTitle, Integer workingDays, List<StudentRow> students) {}

    public record Entry(Long enrollmentId, Integer presentDays) {}

    /** Working and present days for one student (either may be null when not entered). */
    public record Days(Integer working, Integer present) {
        public Integer absent() {
            return working != null && present != null ? Math.max(0, working - present) : null;
        }
    }

    @Transactional(readOnly = true)
    public Sheet sheet(Integer routineId, Integer classId, Integer shiftId, Integer genderSectionId, Long sectionId,
                       Integer groupId, Integer startRoll, Integer endRoll) {
        ExamRoutine routine = routine(routineId);
        Integer yearId = routine.getAcademicYear() != null ? routine.getAcademicYear().getId() : null;
        List<Enrollment> enrollments = enrollmentRepository.findAllByClassIdAndFilters(
                classId, yearId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll);
        Map<Long, Integer> present = presentDays(routineId, enrollments.stream().map(Enrollment::getId).toList());
        List<StudentRow> rows = enrollments.stream()
                .sorted(Comparator.comparing(Enrollment::getClassRoll, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(e -> nvl(e.getStudentSystemId())))
                .map(e -> new StudentRow(
                        e.getId(),
                        e.getStudentSystemId(),
                        e.getStudent() != null ? nvl(e.getStudent().getNameEnglish()) : "",
                        e.getClassRoll(),
                        e.getGenderSection() != null ? e.getGenderSection().getGenderName() : null,
                        e.getSection() != null ? e.getSection().getSectionName() : null,
                        e.getStudentGroup() != null ? e.getStudentGroup().getGroupName() : null,
                        present.get(e.getId())))
                .toList();
        return new Sheet(routine.getId(), routine.getTitle(), routine.getWorkingDays(), rows);
    }

    /**
     * Saves the routine's working days (when {@code setWorkingDays}) and the present days of the listed students.
     * A null presentDays removes that student's entry. Present days can't exceed the working days.
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
        Integer yearId = routine.getAcademicYear() != null ? routine.getAcademicYear().getId() : null;
        Map<Long, Enrollment> enrollments = enrollmentRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Enrollment::getId, Function.identity()));
        for (Long id : ids) {
            Enrollment en = enrollments.get(id);
            if (en == null || en.getAcademicYear() == null || !Objects.equals(en.getAcademicYear().getId(), yearId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A student in the list is not in this exam's academic year");
            }
        }

        Map<Long, ExamAttendance> existing = examAttendanceRepository.findByExamRoutineIdAndEnrollmentIdIn(routineId, ids).stream()
                .collect(Collectors.toMap(ExamAttendance::getEnrollmentId, Function.identity()));
        List<ExamAttendance> toSave = new ArrayList<>();
        List<ExamAttendance> toDelete = new ArrayList<>();
        int changed = 0;
        for (Entry e : list) {
            ExamAttendance row = existing.get(e.enrollmentId());
            if (e.presentDays() == null) {
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

    /** Present days by enrollment for one routine. */
    @Transactional(readOnly = true)
    public Map<Long, Integer> presentDays(Integer routineId, Collection<Long> enrollmentIds) {
        if (enrollmentIds.isEmpty()) return Map.of();
        return examAttendanceRepository.findByExamRoutineIdAndEnrollmentIdIn(routineId, enrollmentIds).stream()
                .collect(Collectors.toMap(ExamAttendance::getEnrollmentId, ExamAttendance::getPresentDays, (a, b) -> a));
    }

    /** Days per routine and enrollment, for the Grand Final card: routineId → enrollmentId → days. */
    @Transactional(readOnly = true)
    public Map<Integer, Map<Long, Days>> daysFor(Collection<Integer> routineIds, Collection<Long> enrollmentIds) {
        Map<Integer, Map<Long, Days>> out = new HashMap<>();
        if (routineIds.isEmpty()) return out;
        Map<Integer, Integer> working = new HashMap<>();
        for (ExamRoutine r : examRoutineRepository.findAllById(routineIds)) working.put(r.getId(), r.getWorkingDays());
        Map<Integer, Map<Long, Integer>> present = new HashMap<>();
        if (!enrollmentIds.isEmpty()) {
            for (ExamAttendance a : examAttendanceRepository.findByExamRoutineIdInAndEnrollmentIdIn(routineIds, enrollmentIds)) {
                present.computeIfAbsent(a.getExamRoutineId(), k -> new HashMap<>()).put(a.getEnrollmentId(), a.getPresentDays());
            }
        }
        for (Integer r : routineIds) {
            Map<Long, Days> m = new HashMap<>();
            for (Long e : enrollmentIds) {
                m.put(e, new Days(working.get(r), present.getOrDefault(r, Map.of()).get(e)));
            }
            out.put(r, m);
        }
        return out;
    }

    /** The routine's working days (null when not set). */
    @Transactional(readOnly = true)
    public Integer workingDays(Integer routineId) {
        return examRoutineRepository.findById(routineId).map(ExamRoutine::getWorkingDays).orElse(null);
    }

    private ExamRoutine routine(Integer id) {
        return examRoutineRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam routine not found"));
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
