package com.example.academic_service.repository;

import com.example.academic_service.entity.FbacRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FbacRoleRepository extends JpaRepository<FbacRole, Integer> {
    List<FbacRole> findByIsActive(Boolean isActive);
    boolean existsByRoleName(String roleName);
}
