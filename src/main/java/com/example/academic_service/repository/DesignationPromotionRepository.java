package com.example.academic_service.repository;

import com.example.academic_service.entity.DesignationPromotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DesignationPromotionRepository extends JpaRepository<DesignationPromotion, Integer> {
    List<DesignationPromotion> findByFromDesignationId(Integer fromDesignationId);
    List<DesignationPromotion> findByToDesignationId(Integer toDesignationId);
}
