package com.example.academic_service.repository;

import com.example.academic_service.entity.LeaveRequest;
import com.example.academic_service.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByStaffIdOrderBySubmittedAtDesc(Long staffId);
    List<LeaveRequest> findByStatusOrderBySubmittedAtDesc(LeaveStatus status);
    List<LeaveRequest> findByStatusInOrderBySubmittedAtDesc(List<LeaveStatus> statuses);
    List<LeaveRequest> findAllByOrderBySubmittedAtDesc();
}
