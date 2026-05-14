package com.example.academic_service.repository;

import com.example.academic_service.entity.ClassRoutine;
import com.example.academic_service.entity.RoutineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface ClassRoutineRepository extends JpaRepository<ClassRoutine, Integer> {

    List<ClassRoutine> findByRoutineTypeAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(RoutineType routineType);

    List<ClassRoutine> findByRoom_IdAndDayOfWeekAndRoutineTypeAndIsActiveTrue(
            Integer roomId, DayOfWeek dayOfWeek, RoutineType routineType);

    List<ClassRoutine> findByClassEntity_IdAndGenderSection_IdAndDayOfWeekAndRoutineTypeAndIsActiveTrue(
            Integer classId, Integer genderSectionId, DayOfWeek dayOfWeek, RoutineType routineType);

    @org.springframework.data.jpa.repository.Query(
        "SELECT cr FROM ClassRoutine cr WHERE cr.isActive = true " +
        "AND cr.routineType = com.example.academic_service.entity.RoutineType.DEFAULT " +
        "AND cr.classEntity.id = :classId " +
        "AND (:genderSectionId IS NULL OR cr.genderSection.id = :genderSectionId) " +
        "AND (:sectionId IS NULL OR cr.section IS NULL OR cr.section.id = :sectionId) " +
        "AND (:groupId IS NULL OR cr.studentGroup IS NULL OR cr.studentGroup.id = :groupId) " +
        "ORDER BY cr.dayOfWeek ASC, cr.startTime ASC")
    List<ClassRoutine> findStudentWeeklySchedule(
            @org.springframework.data.repository.query.Param("classId") Integer classId,
            @org.springframework.data.repository.query.Param("genderSectionId") Integer genderSectionId,
            @org.springframework.data.repository.query.Param("sectionId") Long sectionId,
            @org.springframework.data.repository.query.Param("groupId") Integer groupId);
}
