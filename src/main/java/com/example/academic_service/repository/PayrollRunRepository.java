package com.example.academic_service.repository;

import com.example.academic_service.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {
    List<PayrollRun> findAllByOrderByPeriodDescIdDesc();
    List<PayrollRun> findByRunTypeAndPeriodAndStatusNot(PayrollRunType runType, LocalDate period, PayrollRunStatus status);
}
