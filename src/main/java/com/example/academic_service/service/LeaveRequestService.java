package com.example.academic_service.service;

import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository requestRepository;
    private final LeaveApprovalRecordRepository approvalRecordRepository;
    private final LeaveApprovalTierRepository tierRepository;
    private final StaffRepository staffRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final SystemUserRoleRepository systemUserRoleRepository;
    private final FbacPermissionRepository fbacPermissionRepository;

    public List<LeaveRequest> getByStaff(Long staffId) {
        return requestRepository.findByStaffIdOrderBySubmittedAtDesc(staffId);
    }

    public List<LeaveRequest> getPending() {
        return requestRepository.findByStatusInOrderBySubmittedAtDesc(
                List.of(LeaveStatus.PENDING, LeaveStatus.PARTIALLY_APPROVED));
    }

    public LeaveRequest submit(Long staffId, LeaveRequest req) {
        req.setStaff(staffRepository.getReferenceById(staffId));
        req.setLeaveType(leaveTypeRepository.getReferenceById(req.getLeaveType().getId()));
        req.setStatus(LeaveStatus.PENDING);
        req.setCurrentTierOrder(1);
        req.setSubmittedAt(LocalDateTime.now());
        return requestRepository.save(req);
    }

    @Transactional
    public LeaveRequest processApproval(Long requestId, Long approvingUserId, ApprovalAction action, String remarks) {
        LeaveRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found: " + requestId));

        if (request.getStatus() == LeaveStatus.APPROVED || request.getStatus() == LeaveStatus.REJECTED)
            throw new IllegalStateException("Leave request is already finalized");

        int currentTier = request.getCurrentTierOrder();

        // verify approver has the required role for this tier
        LeaveApprovalTier tier = tierRepository
                .findByLeaveTypeIdAndTierOrder(request.getLeaveType().getId(), currentTier)
                .orElseThrow(() -> new IllegalArgumentException("No tier configured for order: " + currentTier));

        if (tier.getFbacRole() != null) {
            List<Integer> userRoleIds = systemUserRoleRepository.findRoleIdsByUserId(approvingUserId);
            if (!userRoleIds.contains(tier.getFbacRole().getId()))
                throw new IllegalStateException("You are not authorized to approve this tier");
        }

        LeaveApprovalRecord record = new LeaveApprovalRecord();
        record.setLeaveRequest(request);
        record.setTierOrder(currentTier);
        record.setApprovedByUserId(approvingUserId);
        record.setAction(action);
        record.setRemarks(remarks);
        record.setActionAt(LocalDateTime.now());
        approvalRecordRepository.save(record);

        if (action == ApprovalAction.REJECTED) {
            request.setStatus(LeaveStatus.REJECTED);
        } else {
            int totalTiers = tierRepository.countByLeaveTypeId(request.getLeaveType().getId());
            if (currentTier >= totalTiers) {
                request.setStatus(LeaveStatus.APPROVED);
            } else {
                request.setCurrentTierOrder(currentTier + 1);
                request.setStatus(LeaveStatus.PARTIALLY_APPROVED);
            }
        }

        return requestRepository.save(request);
    }
}
