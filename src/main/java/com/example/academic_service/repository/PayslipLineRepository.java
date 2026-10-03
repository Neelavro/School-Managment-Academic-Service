package com.example.academic_service.repository;

import com.example.academic_service.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface PayslipLineRepository extends JpaRepository<PayslipLine, Long> {
    List<PayslipLine> findByPayslipIdInOrderBySortOrderAscIdAsc(Collection<Long> payslipIds);
    List<PayslipLine> findByPayslipIdOrderBySortOrderAscIdAsc(Long payslipId);
    void deleteByPayslipId(Long payslipId);
    long countByComponentId(Long componentId);
}
