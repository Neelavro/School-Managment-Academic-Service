package com.example.academic_service.service;

import com.example.academic_service.entity.Attendance;
import com.example.academic_service.entity.ClassTeacher;
import com.example.academic_service.entity.Enrollment;
import com.example.academic_service.repository.AttendanceRepository;
import com.example.academic_service.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassTeacherService classTeacherService;

    // ── Shared logic ──────────────────────────────────────────────────────────

    /**
     * A register's students: a section's, or — for a class + gender section without sections (sectionId null) —
     * the class + gender section's students who have no section.
     */
    public List<Enrollment> register(Long sectionId, Integer classId, Integer genderSectionId, Integer academicYearId) {
        if (sectionId != null) return enrollmentRepository.findBySectionAndYear(sectionId, academicYearId);
        if (classId == null || genderSectionId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a section, or a class and a gender section");
        return enrollmentRepository.findByClassGenderWithoutSection(classId, genderSectionId, academicYearId);
    }

    public List<Map<String, Object>> getStudentsWithStatus(Long sectionId, Integer classId, Integer genderSectionId,
                                                           Integer academicYearId, LocalDate date) {
        List<Enrollment> enrollments = register(sectionId, classId, genderSectionId, academicYearId);

        List<Long> ids = enrollments.stream().map(Enrollment::getId).toList();
        Map<Long, String> absentSourceById = ids.isEmpty() ? Map.of()
                : attendanceRepository.findByEnrollmentIdsAndDate(ids, date).stream()
                        .collect(Collectors.toMap(Attendance::getEnrollmentId, Attendance::getSource, (a, b) -> a));

        return enrollments.stream()
                .sorted(Comparator.comparingInt(e -> Optional.ofNullable(e.getClassRoll()).orElse(Integer.MAX_VALUE)))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("enrollmentId", e.getId());
                    m.put("studentName", e.getStudent() != null ? e.getStudent().getNameEnglish() : "");
                    m.put("studentSystemId", e.getStudentSystemId());
                    m.put("classRoll", e.getClassRoll());
                    m.put("isAbsent", absentSourceById.containsKey(e.getId()));
                    m.put("source", absentSourceById.get(e.getId()));
                    return m;
                }).toList();
    }

    @Transactional
    public void saveAttendance(Long sectionId, Integer classId, Integer genderSectionId, Integer academicYearId,
                               LocalDate date, List<Long> absentEnrollmentIds) {
        List<Enrollment> enrollments = register(sectionId, classId, genderSectionId, academicYearId);

        List<Long> allIds = enrollments.stream().map(Enrollment::getId).toList();
        if (allIds.isEmpty()) return;

        // Replace: teacher save wipes both MANUAL and STELLER rows for this register+date,
        // then inserts fresh MANUAL rows. Teacher is authoritative.
        attendanceRepository.deleteByEnrollmentIdsAndDate(allIds, date);

        if (absentEnrollmentIds != null && !absentEnrollmentIds.isEmpty()) {
            Set<Long> validIds = new HashSet<>(allIds);
            List<Attendance> toSave = absentEnrollmentIds.stream()
                    .filter(validIds::contains)
                    .map(eid -> {
                        Attendance a = new Attendance();
                        a.setEnrollmentId(eid);
                        a.setDate(date);
                        a.setSource("MANUAL");
                        return a;
                    }).toList();
            attendanceRepository.saveAll(toSave);
        }
    }

    // ── Teacher portal ────────────────────────────────────────────────────────

    public List<Map<String, Object>> getTeacherStudentsWithStatus(Long staffId, Integer academicYearId, LocalDate date) {
        ClassTeacher ct = classTeacherService.getByStaffAndYear(staffId, academicYearId);
        return getStudentsWithStatus(sectionIdOf(ct), classIdOf(ct), genderSectionIdOf(ct), academicYearId, date);
    }

    @Transactional
    public void saveTeacherAttendance(Long staffId, Integer academicYearId, LocalDate date, List<Long> absentEnrollmentIds) {
        ClassTeacher ct = classTeacherService.getByStaffAndYear(staffId, academicYearId);
        saveAttendance(sectionIdOf(ct), classIdOf(ct), genderSectionIdOf(ct), academicYearId, date, absentEnrollmentIds);
    }

    private static Long sectionIdOf(ClassTeacher ct) {
        return ct.getSection() != null ? ct.getSection().getId() : null;
    }

    private static Integer classIdOf(ClassTeacher ct) {
        return ct.getStudentClass() != null ? ct.getStudentClass().getId() : null;
    }

    private static Integer genderSectionIdOf(ClassTeacher ct) {
        return ct.getGenderSection() != null ? ct.getGenderSection().getId() : null;
    }

    // ── Student portal ────────────────────────────────────────────────────────

    public List<String> getMyAbsentDates(Long enrollmentId, int year, int month) {
        return attendanceRepository.findByEnrollmentIdAndMonth(enrollmentId, year, month)
                .stream().map(a -> a.getDate().toString()).toList();
    }
}
