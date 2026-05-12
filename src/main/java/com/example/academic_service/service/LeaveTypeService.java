package com.example.academic_service.service;

import com.example.academic_service.entity.LeaveApprovalTier;
import com.example.academic_service.entity.LeaveType;
import com.example.academic_service.repository.FbacRoleRepository;
import com.example.academic_service.repository.LeaveApprovalTierRepository;
import com.example.academic_service.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveApprovalTierRepository tierRepository;
    private final FbacRoleRepository fbacRoleRepository;

    public List<LeaveType> getAll() { return leaveTypeRepository.findAll(); }
    public List<LeaveType> getActive() { return leaveTypeRepository.findByIsActive(true); }

    public LeaveType create(LeaveType leaveType) {
        leaveType.setIsActive(true);
        return leaveTypeRepository.save(leaveType);
    }

    public LeaveType update(Integer id, LeaveType req) {
        LeaveType existing = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("LeaveType not found: " + id));
        existing.setName(req.getName());
        existing.setAnnualQuota(req.getAnnualQuota());
        return leaveTypeRepository.save(existing);
    }

    public void deactivate(Integer id) {
        LeaveType lt = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("LeaveType not found: " + id));
        lt.setIsActive(false);
        leaveTypeRepository.save(lt);
    }

    public List<LeaveApprovalTier> getTiers(Integer leaveTypeId) {
        return tierRepository.findByLeaveTypeIdOrderByTierOrder(leaveTypeId);
    }

    public LeaveApprovalTier addTier(Integer leaveTypeId, LeaveApprovalTier req) {
        int nextOrder = tierRepository.countByLeaveTypeId(leaveTypeId) + 1;
        req.setLeaveType(leaveTypeRepository.getReferenceById(leaveTypeId));
        req.setTierOrder(nextOrder);
        if (req.getFbacRole() != null && req.getFbacRole().getId() != null)
            req.setFbacRole(fbacRoleRepository.getReferenceById(req.getFbacRole().getId()));
        else
            req.setFbacRole(null);
        return tierRepository.save(req);
    }

    public void deleteTier(Integer tierId) {
        tierRepository.deleteById(tierId);
    }
}
