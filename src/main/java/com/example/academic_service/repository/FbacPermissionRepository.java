package com.example.academic_service.repository;

import com.example.academic_service.entity.FbacPermission;
import com.example.academic_service.entity.Submodule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FbacPermissionRepository extends JpaRepository<FbacPermission, Integer> {
    List<FbacPermission> findByFbacRoleId(Integer fbacRoleId);
    Optional<FbacPermission> findByFbacRoleIdAndSubmodule(Integer fbacRoleId, Submodule submodule);
    void deleteByFbacRoleId(Integer fbacRoleId);

    @Query("SELECT p FROM FbacPermission p WHERE p.fbacRole.id IN :roleIds AND p.submodule = :submodule")
    List<FbacPermission> findByRoleIdsAndSubmodule(List<Integer> roleIds, Submodule submodule);

    // Fetch all permissions for a set of roles in one query
    List<FbacPermission> findByFbacRoleIdIn(List<Integer> roleIds);
}
