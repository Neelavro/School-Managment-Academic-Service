package com.example.academic_service.repository;

import com.example.academic_service.entity.GradingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GradingPolicyRepository extends JpaRepository<GradingPolicy, Long> {
    boolean existsByName(String name);
    List<GradingPolicy> findByIsActive(Boolean isActive);
}