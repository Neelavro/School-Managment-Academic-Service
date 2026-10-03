package com.example.academic_service.repository;

import com.example.academic_service.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface StaffSalaryRepository extends JpaRepository<StaffSalary, Long> {
    List<StaffSalary> findByStaffIdOrderByEffectiveFromAsc(Long staffId);
    List<StaffSalary> findByStaffIdAndEffectiveFrom(Long staffId, LocalDate effectiveFrom);
    List<StaffSalary> findByEffectiveFromLessThanEqual(LocalDate month);
    long countByComponentId(Long componentId);
}
