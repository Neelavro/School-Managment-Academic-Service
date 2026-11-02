package com.example.academic_service.repository;

import com.example.academic_service.entity.ExamAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ExamAttendanceRepository extends JpaRepository<ExamAttendance, Long> {
    List<ExamAttendance> findByExamRoutineIdAndEnrollmentIdIn(Integer examRoutineId, Collection<Long> enrollmentIds);

    List<ExamAttendance> findByExamRoutineIdInAndEnrollmentIdIn(Collection<Integer> examRoutineIds, Collection<Long> enrollmentIds);
}
