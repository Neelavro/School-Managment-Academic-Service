package com.example.academic_service.repository;

import com.example.academic_service.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // Full absent rows (enrollmentId + source) for a set of enrollments on a date.
    // Used by both the student status endpoint and the reconciler's manual check.
    @Query("SELECT a FROM Attendance a WHERE a.enrollmentId IN :enrollmentIds AND a.date = :date")
    List<Attendance> findByEnrollmentIdsAndDate(@Param("enrollmentIds") List<Long> enrollmentIds, @Param("date") LocalDate date);

    // Delete all absence records for a set of enrollments on a date (used by replace strategy)
    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.enrollmentId IN :enrollmentIds AND a.date = :date")
    void deleteByEnrollmentIdsAndDate(@Param("enrollmentIds") List<Long> enrollmentIds, @Param("date") LocalDate date);

    // Reconciler idempotency: wipe only STELLER rows before re-inserting.
    // Manual rows are never touched by the reconciler.
    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.enrollmentId IN :enrollmentIds AND a.date = :date AND a.source = :source")
    void deleteBySourceAndEnrollmentIdsAndDate(@Param("source") String source,
                                               @Param("enrollmentIds") List<Long> enrollmentIds,
                                               @Param("date") LocalDate date);

    // Student portal: absences for one enrollment in a month
    @Query("SELECT a FROM Attendance a WHERE a.enrollmentId = :enrollmentId AND YEAR(a.date) = :year AND MONTH(a.date) = :month ORDER BY a.date")
    List<Attendance> findByEnrollmentIdAndMonth(@Param("enrollmentId") Long enrollmentId, @Param("year") int year, @Param("month") int month);

    // Progress report: absences per enrollment before a date (exclusive): rows of [enrollmentId, count].
    @Query("SELECT a.enrollmentId, COUNT(a) FROM Attendance a WHERE a.enrollmentId IN :enrollmentIds AND a.date < :to GROUP BY a.enrollmentId")
    List<Object[]> countAbsencesBefore(@Param("enrollmentIds") Collection<Long> enrollmentIds, @Param("to") LocalDate to);

    // Progress report: absences per enrollment from a date (inclusive) up to a date (exclusive).
    @Query("SELECT a.enrollmentId, COUNT(a) FROM Attendance a WHERE a.enrollmentId IN :enrollmentIds AND a.date >= :from AND a.date < :to GROUP BY a.enrollmentId")
    List<Object[]> countAbsencesBetween(@Param("enrollmentIds") Collection<Long> enrollmentIds,
                                        @Param("from") LocalDate from, @Param("to") LocalDate to);
}
