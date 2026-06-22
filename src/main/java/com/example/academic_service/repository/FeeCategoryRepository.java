package com.example.academic_service.repository;

import com.example.academic_service.entity.FeeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeCategoryRepository extends JpaRepository<FeeCategory, Long> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByNameIgnoreCase(String name);
    Optional<FeeCategory> findByCodeIgnoreCase(String code);
    List<FeeCategory> findByIsActive(Boolean isActive);
    long countByIncomeLedgerId(Long incomeLedgerId);
}
