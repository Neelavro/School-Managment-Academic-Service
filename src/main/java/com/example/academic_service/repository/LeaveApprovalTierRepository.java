package com.example.academic_service.repository;

import com.example.academic_service.entity.LeaveApprovalTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveApprovalTierRepository extends JpaRepository<LeaveApprovalTier, Integer> {
    List<LeaveApprovalTier> findByLeaveTypeIdOrderByTierOrder(Integer leaveTypeId);
    Optional<LeaveApprovalTier> findByLeaveTypeIdAndTierOrder(Integer leaveTypeId, Integer tierOrder);
    int countByLeaveTypeId(Integer leaveTypeId);
}
