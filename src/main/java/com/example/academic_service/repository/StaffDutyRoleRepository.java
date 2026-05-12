package com.example.academic_service.repository;

import com.example.academic_service.entity.StaffDutyRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffDutyRoleRepository extends JpaRepository<StaffDutyRole, Integer> {
    List<StaffDutyRole> findByIsActive(Boolean isActive);
    boolean existsByRoleName(String roleName);
}
