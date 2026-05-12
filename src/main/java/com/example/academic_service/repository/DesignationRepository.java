package com.example.academic_service.repository;

import com.example.academic_service.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DesignationRepository extends JpaRepository<Designation, Integer> {
    List<Designation> findByIsActive(Boolean isActive);
    boolean existsByName(String name);
}
