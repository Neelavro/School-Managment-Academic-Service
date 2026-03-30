package com.example.academic_service.repository;

import com.example.academic_service.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Integer> {
    List<AcademicYear> findByIsActiveTrue();
}
