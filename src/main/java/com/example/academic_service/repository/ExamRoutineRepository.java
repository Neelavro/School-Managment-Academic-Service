package com.example.academic_service.repository;

import com.example.academic_service.entity.ExamRoutine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRoutineRepository extends JpaRepository<ExamRoutine, Integer> {

    // Active / inactive / all
    List<ExamRoutine> findByIsActiveTrueOrderByCreatedAtDesc();
    List<ExamRoutine> findByIsActiveFalseOrderByCreatedAtDesc();

    // Filtered by academicYear or examType
    List<ExamRoutine> findByAcademicYearIdAndIsActiveTrue(Integer academicYearId);
    List<ExamRoutine> findByExamTypeIdAndIsActiveTrue(Integer examTypeId);

    // Duplicate checks
    boolean existsByExamTypeIdAndAcademicYearIdAndIsActiveTrue(Integer examTypeId, Integer academicYearId);
    boolean existsByExamTypeIdAndAcademicYearIdAndIsActiveTrueAndIdNot(Integer examTypeId, Integer academicYearId, Integer id);
}