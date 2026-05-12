package com.example.academic_service.repository;

import com.example.academic_service.entity.StaffEmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffEmergencyContactRepository extends JpaRepository<StaffEmergencyContact, Long> {
    List<StaffEmergencyContact> findByStaffId(Long staffId);
    void deleteByStaffId(Long staffId);
}
