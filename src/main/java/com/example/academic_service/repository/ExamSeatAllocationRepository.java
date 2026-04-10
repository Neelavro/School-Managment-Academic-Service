package com.example.academic_service.repository;

import com.example.academic_service.entity.ExamSeatAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ExamSeatAllocationRepository extends JpaRepository<ExamSeatAllocation, Integer> {

    List<ExamSeatAllocation> findByExamSessionId(Integer examSessionId);
    // ExamSeatAllocationRepository
    List<ExamSeatAllocation> findByExamSessionIdIn(List<Integer> sessionIds);
    @Query("""
        SELECT a FROM ExamSeatAllocation a
        JOIN a.examSession s
        WHERE s.date = :date
          AND s.startTime < :endTime
          AND s.endTime > :startTime
          AND a.roomId IS NOT NULL
          AND a.startRoll IS NOT NULL
          AND a.endRoll IS NOT NULL
    """)
    List<ExamSeatAllocation> findOverlappingAllocations(
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}