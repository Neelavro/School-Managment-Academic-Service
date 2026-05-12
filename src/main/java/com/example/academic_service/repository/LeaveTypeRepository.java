package com.example.academic_service.repository;

import com.example.academic_service.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Integer> {
    List<LeaveType> findByIsActive(Boolean isActive);
}
