package com.example.academic_service.service;

import com.example.academic_service.entity.LeaveType;
import com.example.academic_service.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;

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
}
