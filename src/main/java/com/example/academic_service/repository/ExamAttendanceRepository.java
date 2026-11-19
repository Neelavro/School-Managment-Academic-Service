package com.example.academic_service.repository;

import com.example.academic_service.entity.ExamAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Collection;
import java.util.List;

public interface ExamAttendanceRepository extends JpaRepository<ExamAttendance, Long> {
    @Modifying
    @Query("DELETE FROM ExamAttendance a WHERE a.enrollmentId = :enrollmentId")
    int deleteAllForEnrollment(@Param("enrollmentId") Long enrollmentId);

    List<ExamAttendance> findByExamRoutineIdAndEnrollmentIdIn(Integer examRoutineId, Collection<Long> enrollmentIds);

    List<ExamAttendance> findByExamRoutineIdInAndEnrollmentIdIn(Collection<Integer> examRoutineIds, Collection<Long> enrollmentIds);
}
