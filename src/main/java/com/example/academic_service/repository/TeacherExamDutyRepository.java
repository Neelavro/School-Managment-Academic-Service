package com.example.academic_service.repository;

import com.example.academic_service.entity.TeacherExamDuty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface TeacherExamDutyRepository extends JpaRepository<TeacherExamDuty, Long> {

    List<TeacherExamDuty> findByStaff_Id(Long staffId);

    boolean existsByStaff_IdAndExamSession_Id(Long staffId, Integer examSessionId);

    @Query("SELECT d FROM TeacherExamDuty d WHERE d.staff.id = :staffId " +
            "AND d.examSession.date = :date " +
            "AND d.examSession.startTime < :endTime " +
            "AND d.examSession.endTime > :startTime")
    List<TeacherExamDuty> findConflicts(
            @Param("staffId") Long staffId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);
}
