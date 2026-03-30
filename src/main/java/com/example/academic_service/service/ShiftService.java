package com.example.academic_service.service;

import com.example.academic_service.entity.Shift;

import java.util.List;

public interface ShiftService {
    public void migrateShift(Shift request);

    Shift createShift(Shift shift);

    Shift updateShift(Long id, Shift shift);

    Shift getShiftById(Long id);

    List<Shift> getAllActiveShifts();

    void deleteShift(Long id); // soft delete
}
