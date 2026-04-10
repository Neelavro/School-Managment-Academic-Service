package com.example.academic_service.repository;

import com.example.academic_service.entity.ExamType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamTypeRepository extends JpaRepository<ExamType, Integer> {
    List<ExamType> findByIsActiveTrueOrderByOrderIndexAsc();
    List<ExamType> findByIsActiveFalseOrderByOrderIndexAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
    boolean existsByNameIgnoreCaseAndIsActiveTrueAndIdNot(String name, Integer id);
}