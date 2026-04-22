package com.example.academic_service.repository;

import com.example.academic_service.entity.Class;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassRepository extends JpaRepository<Class, Integer> {

    List<Class> findAllByShiftIdAndIsActiveTrue(Integer shiftId);
    // In ClassRepository - replace the old method
    List<Class> findAllByShiftIdAndIsActiveTrueOrderByOrderIndex(Integer shiftId);
    Optional<Class> findByIdAndIsActiveTrue(Integer id);
}
