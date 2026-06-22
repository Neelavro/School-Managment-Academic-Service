package com.example.academic_service.repository;

import com.example.academic_service.entity.FeePricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeePricingRepository extends JpaRepository<FeePricing, Long> {
    List<FeePricing> findByFeeCategoryId(Long feeCategoryId);
    Optional<FeePricing> findByFeeCategoryIdAndClassId(Long feeCategoryId, Long classId);
    long countByFeeCategoryId(Long feeCategoryId);
    void deleteByFeeCategoryIdAndClassId(Long feeCategoryId, Long classId);
}
