package com.example.academic_service.service;

import com.example.academic_service.entity.StaffDutyRole;
import com.example.academic_service.entity.StaffDutyRoleAssignment;
import com.example.academic_service.repository.StaffDutyRoleAssignmentRepository;
import com.example.academic_service.repository.StaffDutyRoleRepository;
import com.example.academic_service.repository.StaffRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffDutyRoleService {

    private final StaffDutyRoleRepository dutyRoleRepository;
    private final StaffDutyRoleAssignmentRepository assignmentRepository;
    private final StaffRepository staffRepository;

    public List<StaffDutyRole> getAll() { return dutyRoleRepository.findAll(); }
    public List<StaffDutyRole> getActive() { return dutyRoleRepository.findByIsActive(true); }

    public StaffDutyRole create(StaffDutyRole role) {
        if (dutyRoleRepository.existsByRoleName(role.getRoleName()))
            throw new IllegalArgumentException("Duty role already exists: " + role.getRoleName());
        role.setIsActive(true);
        return dutyRoleRepository.save(role);
    }

    public StaffDutyRole update(Integer id, StaffDutyRole req) {
        StaffDutyRole existing = dutyRoleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Duty role not found: " + id));
        existing.setRoleName(req.getRoleName());
        return dutyRoleRepository.save(existing);
    }

    public void deactivate(Integer id) {
        StaffDutyRole r = dutyRoleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Duty role not found: " + id));
        r.setIsActive(false);
        dutyRoleRepository.save(r);
    }

    public List<StaffDutyRoleAssignment> getAssignmentsForStaff(Long staffId) {
        return assignmentRepository.findByStaffId(staffId);
    }

    @Transactional
    public StaffDutyRoleAssignment assign(Long staffId, Integer dutyRoleId) {
        if (assignmentRepository.existsByStaffIdAndDutyRoleId(staffId, dutyRoleId))
            throw new IllegalArgumentException("Already assigned");
        StaffDutyRoleAssignment a = new StaffDutyRoleAssignment();
        a.setStaff(staffRepository.getReferenceById(staffId));
        a.setDutyRole(dutyRoleRepository.getReferenceById(dutyRoleId));
        return assignmentRepository.save(a);
    }

    @Transactional
    public void unassign(Long staffId, Integer dutyRoleId) {
        assignmentRepository.deleteByStaffIdAndDutyRoleId(staffId, dutyRoleId);
    }
}
