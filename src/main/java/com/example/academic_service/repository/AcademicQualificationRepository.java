package com.example.academic_service.repository;

import com.example.academic_service.entity.AcademicQualification;
import com.example.academic_service.entity.QualificationLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicQualificationRepository extends JpaRepository<AcademicQualification, Long> {
    List<AcademicQualification> findByStaffIdOrderByLevel(Long staffId);
    void deleteByStaffId(Long staffId);
    boolean existsByStaffIdAndLevel(Long staffId, QualificationLevel level);
}
