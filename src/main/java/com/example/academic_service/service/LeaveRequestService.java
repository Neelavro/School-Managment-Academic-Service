package com.example.academic_service.service;

import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository requestRepository;
    private final LeaveApprovalRecordRepository approvalRecordRepository;
    private final DesignationApprovalChainRepository chainRepository;
    private final LeaveBalanceRepository balanceRepository;
    private final StaffRepository staffRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final SystemUserRoleRepository systemUserRoleRepository;

    public List<LeaveRequest> getAll(String status) {
        if (status != null)
            return requestRepository.findByStatusOrderBySubmittedAtDesc(LeaveStatus.valueOf(status));
        return requestRepository.findAllByOrderBySubmittedAtDesc();
    }

    public List<LeaveRequest> getByStaff(Long staffId) {
        return requestRepository.findByStaffIdOrderBySubmittedAtDesc(staffId);
    }

    public List<LeaveRequest> getPending() {
        return requestRepository.findByStatusInOrderBySubmittedAtDesc(
                List.of(LeaveStatus.PENDING, LeaveStatus.PARTIALLY_APPROVED));
    }

    // Returns only requests where the calling user's roles match the current tier's required role
    public List<LeaveRequest> getMyPendingApprovals(Long userId) {
        List<Integer> userRoleIds = systemUserRoleRepository.findRoleIdsByUserId(userId);
        if (userRoleIds.isEmpty()) return List.of();

        List<DesignationApprovalChain> relevantTiers = chainRepository.findByApproverRoleIdIn(userRoleIds);
        if (relevantTiers.isEmpty()) return List.of();

        List<LeaveRequest> pending = requestRepository.findByStatusInOrderBySubmittedAtDesc(
                List.of(LeaveStatus.PENDING, LeaveStatus.PARTIALLY_APPROVED));

        return pending.stream().filter(req -> {
            if (req.getStaff().getCurrentDesignation() == null) return false;
            Integer designationId = req.getStaff().getCurrentDesignation().getId();
            int currentTier = req.getCurrentTierOrder();
            return relevantTiers.stream().anyMatch(t ->
                    t.getDesignation().getId().equals(designationId) &&
                    t.getTierOrder().equals(currentTier) &&
                    userRoleIds.contains(t.getApproverRole() != null ? t.getApproverRole().getId() : -1));
        }).collect(Collectors.toList());
    }

    @Transactional
    public LeaveRequest submit(Long staffId, Integer leaveTypeId, LocalDate startDate, LocalDate endDate, String reason) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
        LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Leave type not found: " + leaveTypeId));

        int totalDays = (int) startDate.datesUntil(endDate.plusDays(1)).count();
        int year = startDate.getYear();

        // Get or auto-create balance row for this leave type + year
        LeaveBalance balance = balanceRepository
                .findByStaffIdAndLeaveTypeIdAndYear(staffId, leaveTypeId, year)
                .orElseGet(() -> {
                    LeaveBalance b = new LeaveBalance();
                    b.setStaff(staff);
                    b.setLeaveType(leaveType);
                    b.setYear(year);
                    b.setAllocatedDays(leaveType.getAnnualQuota() != null ? leaveType.getAnnualQuota() : 0);
                    b.setUsedDays(0);
                    b.setPendingDays(0);
                    return balanceRepository.save(b);
                });

        int available = balance.getAllocatedDays() - balance.getUsedDays() - balance.getPendingDays();
        if (available < totalDays)
            throw new IllegalStateException("Insufficient leave balance. Available: " + available + " days, Requested: " + totalDays + " days.");

        // Determine approval chain from designation
        int chainCount = staff.getCurrentDesignation() != null
                ? chainRepository.countByDesignationId(staff.getCurrentDesignation().getId()) : 0;

        LeaveRequest req = new LeaveRequest();
        req.setStaff(staff);
        req.setLeaveType(leaveType);
        req.setStartDate(startDate);
        req.setEndDate(endDate);
        req.setReason(reason);
        req.setTotalDays(totalDays);
        req.setSubmittedAt(LocalDateTime.now());
        req.setCurrentTierOrder(1);

        if (chainCount == 0) {
            // No approval chain configured — auto-approve
            req.setStatus(LeaveStatus.APPROVED);
            balance.setUsedDays(balance.getUsedDays() + totalDays);
        } else {
            req.setStatus(LeaveStatus.PENDING);
            balance.setPendingDays(balance.getPendingDays() + totalDays);
        }

        balanceRepository.save(balance);
        return requestRepository.save(req);
    }

    @Transactional
    public LeaveRequest processApproval(Long requestId, Long approvingUserId, ApprovalAction action, String remarks) {
        LeaveRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found: " + requestId));

        if (request.getStatus() == LeaveStatus.APPROVED || request.getStatus() == LeaveStatus.REJECTED)
            throw new IllegalStateException("Leave request is already finalized");

        int currentTier = request.getCurrentTierOrder();
        Integer designationId = request.getStaff().getCurrentDesignation() != null
                ? request.getStaff().getCurrentDesignation().getId() : null;

        if (designationId != null) {
            DesignationApprovalChain tier = chainRepository
                    .findByDesignationIdAndTierOrder(designationId, currentTier)
                    .orElseThrow(() -> new IllegalArgumentException("No approval tier configured for order: " + currentTier));

            if (tier.getApproverRole() != null) {
                List<Integer> userRoleIds = systemUserRoleRepository.findRoleIdsByUserId(approvingUserId);
                if (!userRoleIds.contains(tier.getApproverRole().getId()))
                    throw new IllegalStateException("You are not authorized to approve at this tier");
            }
        }

        LeaveApprovalRecord record = new LeaveApprovalRecord();
        record.setLeaveRequest(request);
        record.setTierOrder(currentTier);
        record.setApprovedByUserId(approvingUserId);
        record.setAction(action);
        record.setRemarks(remarks);
        record.setActionAt(LocalDateTime.now());
        approvalRecordRepository.save(record);

        LeaveBalance balance = balanceRepository
                .findByStaffIdAndLeaveTypeIdAndYear(
                        request.getStaff().getId(),
                        request.getLeaveType().getId(),
                        request.getStartDate().getYear())
                .orElse(null);

        if (action == ApprovalAction.REJECTED) {
            request.setStatus(LeaveStatus.REJECTED);
            if (balance != null) {
                balance.setPendingDays(Math.max(0, balance.getPendingDays() - request.getTotalDays()));
                balanceRepository.save(balance);
            }
        } else {
            int totalTiers = designationId != null ? chainRepository.countByDesignationId(designationId) : 0;
            if (currentTier >= totalTiers) {
                request.setStatus(LeaveStatus.APPROVED);
                if (balance != null) {
                    balance.setPendingDays(Math.max(0, balance.getPendingDays() - request.getTotalDays()));
                    balance.setUsedDays(balance.getUsedDays() + request.getTotalDays());
                    balanceRepository.save(balance);
                }
            } else {
                request.setCurrentTierOrder(currentTier + 1);
                request.setStatus(LeaveStatus.PARTIALLY_APPROVED);
            }
        }

        return requestRepository.save(request);
    }
}
