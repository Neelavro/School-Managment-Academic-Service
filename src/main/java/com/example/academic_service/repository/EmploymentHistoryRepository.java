package com.example.academic_service.repository;

import com.example.academic_service.entity.EmploymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmploymentHistoryRepository extends JpaRepository<EmploymentHistory, Long> {
    List<EmploymentHistory> findByStaffIdOrderByFromDateDesc(Long staffId);
    void deleteByStaffId(Long staffId);
}
