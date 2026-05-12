package com.example.academic_service.repository;

import com.example.academic_service.entity.StaffDependent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffDependentRepository extends JpaRepository<StaffDependent, Long> {
    List<StaffDependent> findByStaffId(Long staffId);
    void deleteByStaffId(Long staffId);
}
