package com.example.academic_service.repository;

import com.example.academic_service.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Integer> {
    List<Shift> findAllByIsActiveTrue();

    Optional<Shift> findByIdAndIsActiveTrue(Long id);
}
