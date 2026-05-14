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
}
