package com.example.academic_service.service;

import com.example.academic_service.entity.Attendance;
import com.example.academic_service.entity.ClassTeacher;
import com.example.academic_service.entity.Enrollment;
import com.example.academic_service.repository.AttendanceRepository;
import com.example.academic_service.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassTeacherService classTeacherService;

    // ── Shared logic ──────────────────────────────────────────────────────────

    public List<Map<String, Object>> getStudentsWithStatus(Long sectionId, Integer academicYearId, LocalDate date) {
        List<Enrollment> enrollments = enrollmentRepository.findBySectionAndYear(sectionId, academicYearId);

        List<Long> ids = enrollments.stream().map(Enrollment::getId).toList();
        Set<Long> absentIds = ids.isEmpty() ? Set.of()
                : attendanceRepository.findAbsentEnrollmentIds(ids, date);

        return enrollments.stream()
                .sorted(Comparator.comparingInt(e -> Optional.ofNullable(e.getClassRoll()).orElse(Integer.MAX_VALUE)))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("enrollmentId", e.getId());
                    m.put("studentName", e.getStudent() != null ? e.getStudent().getNameEnglish() : "");
                    m.put("studentSystemId", e.getStudentSystemId());
                    m.put("classRoll", e.getClassRoll());
                    m.put("isAbsent", absentIds.contains(e.getId()));
                    return m;
                }).toList();
    }

    @Transactional
    public void saveAttendance(Long sectionId, Integer academicYearId, LocalDate date, List<Long> absentEnrollmentIds) {
        List<Enrollment> enrollments = enrollmentRepository.findBySectionAndYear(sectionId, academicYearId);

        List<Long> allIds = enrollments.stream().map(Enrollment::getId).toList();
        if (allIds.isEmpty()) return;

        // Replace: delete existing absences for this section+date, then insert new ones
        attendanceRepository.deleteByEnrollmentIdsAndDate(allIds, date);

        if (absentEnrollmentIds != null && !absentEnrollmentIds.isEmpty()) {
            Set<Long> validIds = new HashSet<>(allIds);
            List<Attendance> toSave = absentEnrollmentIds.stream()
                    .filter(validIds::contains)
                    .map(eid -> {
                        Attendance a = new Attendance();
                        a.setEnrollmentId(eid);
                        a.setDate(date);
                        return a;
                    }).toList();
            attendanceRepository.saveAll(toSave);
        }
    }

    // ── Teacher portal ────────────────────────────────────────────────────────

    public List<Map<String, Object>> getTeacherStudentsWithStatus(Long staffId, Integer academicYearId, LocalDate date) {
        ClassTeacher ct = classTeacherService.getByStaffAndYear(staffId, academicYearId);
        return getStudentsWithStatus(ct.getSection().getId(), academicYearId, date);
    }

    @Transactional
    public void saveTeacherAttendance(Long staffId, Integer academicYearId, LocalDate date, List<Long> absentEnrollmentIds) {
        ClassTeacher ct = classTeacherService.getByStaffAndYear(staffId, academicYearId);
        saveAttendance(ct.getSection().getId(), academicYearId, date, absentEnrollmentIds);
    }

    // ── Student portal ────────────────────────────────────────────────────────

    public List<String> getMyAbsentDates(Long enrollmentId, int year, int month) {
        return attendanceRepository.findByEnrollmentIdAndMonth(enrollmentId, year, month)
                .stream().map(a -> a.getDate().toString()).toList();
    }
}
