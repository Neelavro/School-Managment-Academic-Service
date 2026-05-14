package com.example.academic_service.service;

import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LeavePolicyService {
    private final LeavePolicyRepository policyRepository;
    private final DesignationRepository designationRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final StaffRepository staffRepository;
    private final LeaveBalanceRepository balanceRepository;

    public List<LeavePolicy> getAll() { return policyRepository.findAll(); }

    public List<LeavePolicy> getByDesignation(Integer designationId) {
        return policyRepository.findByDesignationId(designationId);
    }

    @Transactional
    public LeavePolicy save(Integer designationId, Integer leaveTypeId, int annualDays) {
        LeavePolicy policy = policyRepository
                .findByDesignationIdAndLeaveTypeId(designationId, leaveTypeId)
                .orElse(new LeavePolicy());
        policy.setDesignation(designationRepository.findById(designationId)
                .orElseThrow(() -> new IllegalArgumentException("Designation not found: " + designationId)));
        policy.setLeaveType(leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new IllegalArgumentException("LeaveType not found: " + leaveTypeId)));
        policy.setAnnualDays(annualDays);
        policy.setIsActive(true);
        return policyRepository.save(policy);
    }

    public void delete(Integer id) { policyRepository.deleteById(id); }

    // Initialize leave balances for all staff for a given year based on their current designation
    @Transactional
    public Map<String, Object> initializeBalances(Integer year) {
        List<Staff> allStaff = staffRepository.findAll();
        int created = 0, skipped = 0;
        for (Staff staff : allStaff) {
            if (staff.getCurrentDesignation() == null) { skipped++; continue; }
            List<LeavePolicy> policies = policyRepository.findByDesignationId(staff.getCurrentDesignation().getId());
            for (LeavePolicy policy : policies) {
                boolean exists = balanceRepository
                        .findByStaffIdAndLeaveTypeIdAndYear(staff.getId(), policy.getLeaveType().getId(), year)
                        .isPresent();
                if (exists) { skipped++; continue; }
                LeaveBalance balance = new LeaveBalance();
                balance.setStaff(staff);
                balance.setLeaveType(policy.getLeaveType());
                balance.setYear(year);
                balance.setAllocatedDays(policy.getAnnualDays());
                balanceRepository.save(balance);
                created++;
            }
        }
        return Map.of("created", created, "skipped", skipped, "year", year);
    }

    // Initialize balances for a single staff member
    @Transactional
    public void initializeForStaff(Long staffId, Integer year) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
        if (staff.getCurrentDesignation() == null) return;
        List<LeavePolicy> policies = policyRepository.findByDesignationId(staff.getCurrentDesignation().getId());
        for (LeavePolicy policy : policies) {
            balanceRepository.findByStaffIdAndLeaveTypeIdAndYear(staffId, policy.getLeaveType().getId(), year)
                    .ifPresentOrElse(b -> {}, () -> {
                        LeaveBalance balance = new LeaveBalance();
                        balance.setStaff(staff);
                        balance.setLeaveType(policy.getLeaveType());
                        balance.setYear(year);
                        balance.setAllocatedDays(policy.getAnnualDays());
                        balanceRepository.save(balance);
                    });
        }
    }
}
