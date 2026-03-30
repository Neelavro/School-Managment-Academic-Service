package com.example.academic_service.repository;

import com.example.academic_service.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long>,
        JpaSpecificationExecutor<Enrollment> {

    // Check duplicate enrollment in same academic year
    boolean existsByStudentSystemIdAndAcademicYearId(String studentSystemId, Integer academicYearId);

    // Find a specific student's enrollment in a given academic year
    Optional<Enrollment> findByStudentSystemIdAndAcademicYearId(String studentSystemId, Integer academicYearId);

    // Find all enrollments for a student across all years
    List<Enrollment> findByStudentSystemId(String studentSystemId);

    // Find all enrollments for a student that are active
    List<Enrollment> findByStudentSystemIdAndIsActive(String studentSystemId, Boolean isActive);
}