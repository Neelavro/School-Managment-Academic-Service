package com.example.academic_service.repository;

import com.example.academic_service.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    boolean existsByNameAndGradingPolicyId(String name, Long gradingPolicyId);
    List<Grade> findByGradingPolicyId(Long gradingPolicyId);
}