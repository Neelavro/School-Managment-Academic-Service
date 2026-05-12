package com.example.academic_service.repository;

import com.example.academic_service.entity.StaffClassAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffClassAssignmentRepository extends JpaRepository<StaffClassAssignment, Long> {
    List<StaffClassAssignment> findByStaffId(Long staffId);
    List<StaffClassAssignment> findByStaffIdAndAcademicYearId(Long staffId, Integer academicYearId);
    List<StaffClassAssignment> findByAcademicYearIdAndAssignedClassId(Integer academicYearId, Integer classId);
}
