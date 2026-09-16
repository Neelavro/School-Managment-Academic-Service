package com.example.academic_service.service;

import com.example.academic_service.entity.AcademicYear;
import com.example.academic_service.entity.Attendance;
import com.example.academic_service.entity.Enrollment;
import com.example.academic_service.repository.AcademicYearRepository;
import com.example.academic_service.repository.AttendanceRepository;
import com.example.academic_service.repository.EnrollmentRepository;
import com.example.academic_service.repository.StellarAttendanceLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bridges Steller device punches into the manual `attendance` table.
 *
 * For each section active in the current academic year on the target date:
 *   - If a teacher has already saved MANUAL rows for that section+date → skip.
 *     Teacher wins.
 *   - Else: wipe existing STELLER rows for that section+date and insert fresh
 *     STELLER rows for every enrolled student whose student_system_id did NOT
 *     appear in stellar_attendance_log for that date.
 *
 * Idempotent — safe to call repeatedly from the hourly Steller sync.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceReconciler {

    private static final String SOURCE_STELLER = "STELLER";
    private static final String SOURCE_MANUAL = "MANUAL";

    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final StellarAttendanceLogRepository stellarLogRepository;
    private final AcademicYearRepository academicYearRepository;

    @Transactional
    public ReconcileResult reconcileDate(LocalDate date) {
        if (!isSchoolDay(date)) {
            return new ReconcileResult(0, 0, 0, "not-a-school-day");
        }

        Optional<AcademicYear> activeYearOpt = academicYearRepository.findFirstByIsActiveTrue();
        if (activeYearOpt.isEmpty()) {
            return new ReconcileResult(0, 0, 0, "no-active-academic-year");
        }
        Integer activeYearId = activeYearOpt.get().getId();

        Set<String> punchedSystemIds = stellarLogRepository.findDistinctRegistrationIdsOnDate(date);

        List<Enrollment> enrollments = enrollmentRepository.findByAcademicYearIdAndIsActiveTrue(activeYearId);
        if (enrollments.isEmpty()) {
            return new ReconcileResult(0, 0, 0, "no-active-enrollments");
        }

        // Group by section so we can honor the "teacher already saved this section" rule per-section.
        Map<Long, List<Enrollment>> bySection = enrollments.stream()
                .filter(e -> e.getSection() != null)
                .collect(Collectors.groupingBy(e -> e.getSection().getId()));

        int sectionsSkipped = 0;
        int sectionsProcessed = 0;
        int rowsInserted = 0;

        for (Map.Entry<Long, List<Enrollment>> entry : bySection.entrySet()) {
            List<Enrollment> sectionEnrollments = entry.getValue();
            List<Long> enrollmentIds = sectionEnrollments.stream().map(Enrollment::getId).toList();

            List<Attendance> existing = attendanceRepository.findByEnrollmentIdsAndDate(enrollmentIds, date);
            boolean hasManual = existing.stream().anyMatch(a -> SOURCE_MANUAL.equals(a.getSource()));
            if (hasManual) {
                sectionsSkipped++;
                continue;
            }

            // Wipe prior STELLER rows so re-runs don't accumulate.
            attendanceRepository.deleteBySourceAndEnrollmentIdsAndDate(SOURCE_STELLER, enrollmentIds, date);

            List<Attendance> toInsert = sectionEnrollments.stream()
                    .filter(e -> e.getStudentSystemId() != null
                            && !punchedSystemIds.contains(e.getStudentSystemId()))
                    .map(e -> {
                        Attendance a = new Attendance();
                        a.setEnrollmentId(e.getId());
                        a.setDate(date);
                        a.setSource(SOURCE_STELLER);
                        return a;
                    })
                    .toList();

            if (!toInsert.isEmpty()) {
                attendanceRepository.saveAll(toInsert);
                rowsInserted += toInsert.size();
            }
            sectionsProcessed++;
        }

        log.info("Steller reconcile {} — sections processed={} skipped(manual)={} rowsInserted={} punchedIds={}",
                date, sectionsProcessed, sectionsSkipped, rowsInserted, punchedSystemIds.size());

        return new ReconcileResult(sectionsProcessed, sectionsSkipped, rowsInserted, "ok");
    }

    // Placeholder for the deferred weekend/holiday feature. Once configurable
    // weekends + holidays land, this should return false on those dates so
    // students aren't marked absent on non-teaching days.
    private boolean isSchoolDay(LocalDate date) {
        return true;
    }

    public record ReconcileResult(int sectionsProcessed, int sectionsSkipped, int rowsInserted, String status) {}
}
