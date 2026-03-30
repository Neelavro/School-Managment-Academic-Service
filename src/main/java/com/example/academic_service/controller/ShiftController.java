package com.example.academic_service.controller;

import com.example.academic_service.entity.Shift;
import com.example.academic_service.service.ShiftService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @PostMapping("/migrate")
    public ResponseEntity<?> migrate(@RequestBody Shift request) {
        shiftService.migrateShift(request);
        return ResponseEntity.ok("Migrated");
    }

    @PostMapping
    public ResponseEntity<Shift> create(@RequestBody Shift shift) {
        return ResponseEntity.ok(shiftService.createShift(shift));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Shift> update(
            @PathVariable Long id,
            @RequestBody Shift shift
    ) {
        return ResponseEntity.ok(shiftService.updateShift(id, shift));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Shift> getById(@PathVariable Long id) {
        return ResponseEntity.ok(shiftService.getShiftById(id));
    }

    @GetMapping
    public ResponseEntity<List<Shift>> getAll() {
        return ResponseEntity.ok(shiftService.getAllActiveShifts());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        shiftService.deleteShift(id);
        return ResponseEntity.noContent().build();
    }
}
