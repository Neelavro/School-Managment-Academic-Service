package com.example.academic_service.repository;

import com.example.academic_service.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface DesignationSalaryRepository extends JpaRepository<DesignationSalary, Long> {
    List<DesignationSalary> findByDesignationId(Integer designationId);
    List<DesignationSalary> findByComponentId(Long componentId);
}
