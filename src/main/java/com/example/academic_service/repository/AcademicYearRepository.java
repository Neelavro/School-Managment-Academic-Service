package com.example.academic_service.repository;

import com.example.academic_service.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Integer> {

    Optional<AcademicYear> findFirstByIsActiveTrue();

    @Modifying
    @Query("UPDATE AcademicYear a SET a.isActive = false")
    void deactivateAll();
}
