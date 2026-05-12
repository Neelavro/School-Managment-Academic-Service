package com.example.academic_service.repository;

import com.example.academic_service.entity.EmployeeType;
import com.example.academic_service.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByStaffSystemId(String staffSystemId);
    boolean existsByStaffSystemId(String staffSystemId);
    List<Staff> findByIsActive(Boolean isActive);
    List<Staff> findByEmployeeType(EmployeeType employeeType);
    List<Staff> findByIsActiveAndEmployeeType(Boolean isActive, EmployeeType employeeType);

    @Query("SELECT MAX(s.staffSystemId) FROM Staff s WHERE s.staffSystemId LIKE :prefix%")
    String findMaxStaffSystemIdByPrefix(String prefix);
}
