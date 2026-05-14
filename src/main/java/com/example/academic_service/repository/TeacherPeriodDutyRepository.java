package com.example.academic_service.repository;

import com.example.academic_service.entity.TeacherPeriodDuty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface TeacherPeriodDutyRepository extends JpaRepository<TeacherPeriodDuty, Long> {

    List<TeacherPeriodDuty> findByStaff_Id(Long staffId);

    boolean existsByClassRoutine_Id(Integer classRoutineId);

    @Query("SELECT d FROM TeacherPeriodDuty d WHERE d.staff.id = :staffId " +
            "AND d.classRoutine.dayOfWeek = :dayOfWeek " +
            "AND d.classRoutine.startTime < :endTime " +
            "AND d.classRoutine.endTime > :startTime")
    List<TeacherPeriodDuty> findConflicts(
            @Param("staffId") Long staffId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);
}
