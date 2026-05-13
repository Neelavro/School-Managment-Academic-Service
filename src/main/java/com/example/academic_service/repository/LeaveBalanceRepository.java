package com.example.academic_service.repository;

import com.example.academic_service.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    List<LeaveBalance> findByStaffIdAndYear(Long staffId, Integer year);
    Optional<LeaveBalance> findByStaffIdAndLeaveTypeIdAndYear(Long staffId, Integer leaveTypeId, Integer year);
    List<LeaveBalance> findByYear(Integer year);
}
