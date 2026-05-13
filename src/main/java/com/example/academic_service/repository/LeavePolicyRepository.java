package com.example.academic_service.repository;

import com.example.academic_service.entity.LeavePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LeavePolicyRepository extends JpaRepository<LeavePolicy, Integer> {
    List<LeavePolicy> findByDesignationId(Integer designationId);
    List<LeavePolicy> findByLeaveTypeId(Integer leaveTypeId);
    Optional<LeavePolicy> findByDesignationIdAndLeaveTypeId(Integer designationId, Integer leaveTypeId);
    boolean existsByDesignationIdAndLeaveTypeId(Integer designationId, Integer leaveTypeId);
}
