package com.example.academic_service.repository;

import com.example.academic_service.entity.SystemUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SystemUserRoleRepository extends JpaRepository<SystemUserRole, Long> {
    List<SystemUserRole> findBySystemUserId(Long systemUserId);
    void deleteBySystemUserId(Long systemUserId);

    @Query("SELECT sur.fbacRole.id FROM SystemUserRole sur WHERE sur.systemUser.id = :userId")
    List<Integer> findRoleIdsByUserId(Long userId);
}
