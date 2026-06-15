package com.example.academic_service.repository;

import com.example.academic_service.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // Returns absent enrollment IDs for a given set of enrollments on a date
    @Query("SELECT a.enrollmentId FROM Attendance a WHERE a.enrollmentId IN :enrollmentIds AND a.date = :date")
    Set<Long> findAbsentEnrollmentIds(@Param("enrollmentIds") List<Long> enrollmentIds, @Param("date") LocalDate date);

    // Delete all absence records for a set of enrollments on a date (used by replace strategy)
    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.enrollmentId IN :enrollmentIds AND a.date = :date")
    void deleteByEnrollmentIdsAndDate(@Param("enrollmentIds") List<Long> enrollmentIds, @Param("date") LocalDate date);

    // Student portal: absences for one enrollment in a month
    @Query("SELECT a FROM Attendance a WHERE a.enrollmentId = :enrollmentId AND YEAR(a.date) = :year AND MONTH(a.date) = :month ORDER BY a.date")
    List<Attendance> findByEnrollmentIdAndMonth(@Param("enrollmentId") Long enrollmentId, @Param("year") int year, @Param("month") int month);
}
