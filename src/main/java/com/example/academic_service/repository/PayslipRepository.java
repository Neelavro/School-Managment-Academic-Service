package com.example.academic_service.repository;

import com.example.academic_service.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    List<Payslip> findByPayrollRunIdOrderByStaffNameAsc(Long payrollRunId);
    List<Payslip> findByPayrollRunIdIn(Collection<Long> runIds);
    List<Payslip> findByStaffIdOrderByIdDesc(Long staffId);
}
