package com.example.academic_service.repository;

import com.example.academic_service.entity.SystemUser;
import com.example.academic_service.entity.UserType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SystemUserRepository extends JpaRepository<SystemUser, Long> {
    Optional<SystemUser> findByPhone(String phone);
    Optional<SystemUser> findByStaffId(Long staffId);
    boolean existsByPhone(String phone);
    boolean existsByUserType(UserType userType);
    List<SystemUser> findByIsActiveAndIsSuspended(Boolean isActive, Boolean isSuspended);
}
