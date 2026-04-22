package com.example.academic_service.repository;

import com.example.academic_service.entity.ExamSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, Integer> {

    // Active / inactive / all
    List<ExamSession> findByExamRoutineId(Integer examRoutineId);
    List<ExamSession> findByExamRoutineIdAndIsActiveTrue(Integer examRoutineId);
    List<ExamSession> findByExamRoutineIdAndIsActiveFalse(Integer examRoutineId);

    // repository/ExamSessionRepository.java
    @Query("SELECT es FROM ExamSession es WHERE es.date = :date " +
            "AND es.startTime < :endTime AND es.endTime > :startTime " +
            "AND es.isActive = true")
    List<ExamSession> findOverlappingSessionsOnDate(
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    Optional<ExamSession> findByIdAndIsActiveTrue(Integer id);

    List<ExamSession> findAllByExamRoutineIdAndExamClassIdAndIsActiveTrue(
            Integer examRoutineId, Integer classId);
}