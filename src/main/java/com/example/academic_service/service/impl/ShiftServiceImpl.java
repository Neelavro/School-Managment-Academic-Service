package com.example.academic_service.service.impl;

import com.example.academic_service.entity.Shift;
import com.example.academic_service.repository.ShiftRepository;
import com.example.academic_service.service.ShiftService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShiftServiceImpl implements ShiftService {

    private final ShiftRepository shiftRepository;

    public ShiftServiceImpl(ShiftRepository shiftRepository) {
        this.shiftRepository = shiftRepository;
    }
    @PersistenceContext
    private EntityManager entityManager;
    @Override
    @Transactional
    public void migrateShift(Shift request) {
        entityManager.createNativeQuery(
                        "INSERT INTO shift (id, name, is_active) VALUES (:id, :name, :isActive)"
                )
                .setParameter("id", request.getId())
                .setParameter("name", request.getName())
                .setParameter("isActive", request.getIsActive())
                .executeUpdate();
    }
    @Override
    public Shift createShift(Shift shift) {
        shift.setIsActive(true);
        return shiftRepository.save(shift);
    }

    @Override
    public Shift updateShift(Long id, Shift shift) {
        Shift existing = getShiftById(id);
        existing.setName(shift.getName());
        existing.setIsActive(shift.getIsActive());
        return shiftRepository.save(existing);
    }

    @Override
    public Shift getShiftById(Long id) {
        return shiftRepository.findById(Math.toIntExact(id))
                .orElseThrow(() -> new RuntimeException("Shift not found"));
    }

    @Override
    public List<Shift> getAllActiveShifts() {
        return shiftRepository.findAllByIsActiveTrue();
    }

    @Override
    public void deleteShift(Long id) {
        Shift shift = shiftRepository.findById(Math.toIntExact(id))
                .orElseThrow(() -> new RuntimeException("Shift not found"));

        shift.setIsActive(false);
        shiftRepository.save(shift);
    }}
