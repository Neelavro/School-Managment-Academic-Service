package com.example.academic_service.controller;

import com.example.academic_service.entity.AcademicYear;
import com.example.academic_service.service.AcademicYearService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-years")
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    public AcademicYearController(AcademicYearService academicYearService) {
        this.academicYearService = academicYearService;
    }

    @PostMapping("/migrate")
    public ResponseEntity<?> migrate(@RequestBody AcademicYear request) {
        academicYearService.migrateAcademicYear(request);
        return ResponseEntity.ok("Migrated");
    }

    // Create
    @PostMapping
    public ResponseEntity<AcademicYear> createAcademicYear(@RequestBody AcademicYear academicYear) {
        AcademicYear created = academicYearService.createAcademicYear(academicYear);
        return ResponseEntity.ok(created);
    }

    // Read all
    @GetMapping
    public ResponseEntity<List<AcademicYear>> getAllAcademicYears() {
        List<AcademicYear> years = academicYearService.getAllAcademicYears();
        return ResponseEntity.ok(years);
    }

    // Read by ID
    @GetMapping("/{id}")
    public ResponseEntity<AcademicYear> getAcademicYearById(@PathVariable Integer id) {
        AcademicYear year = academicYearService.getAcademicYearById(id);
        if (year == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(year);
    }

    // Update
    @PutMapping("/{id}")
    public ResponseEntity<AcademicYear> updateAcademicYear(@PathVariable Integer id, @RequestBody AcademicYear academicYear) {
        AcademicYear updated = academicYearService.updateAcademicYear(id, academicYear);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAcademicYear(@PathVariable Integer id) {
        academicYearService.deleteAcademicYear(id);
        return ResponseEntity.noContent().build();
    }
}
