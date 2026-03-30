package com.example.academic_service.controller;

import com.example.academic_service.entity.Gender;
import com.example.academic_service.service.GenderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genders")
public class GenderController {

    private final GenderService genderService;

    public GenderController(GenderService genderService) {
        this.genderService = genderService;
    }

    // Create
    @PostMapping
    public ResponseEntity<Gender> createGender(@RequestBody Gender gender) {
        Gender created = genderService.createGender(gender);
        return ResponseEntity.ok(created);
    }

    // Read all
    @GetMapping
    public ResponseEntity<List<Gender>> getAllGenders() {
        List<Gender> genders = genderService.getAllGenders();
        return ResponseEntity.ok(genders);
    }

    // Read by ID
    @GetMapping("/{id}")
    public ResponseEntity<Gender> getGenderById(@PathVariable Integer id) {
        Gender gender = genderService.getGenderById(id);
        if (gender == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(gender);
    }

    // Update
    @PutMapping("/{id}")
    public ResponseEntity<Gender> updateGender(@PathVariable Integer id, @RequestBody Gender gender) {
        Gender updated = genderService.updateGender(id, gender);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGender(@PathVariable Integer id) {
        genderService.deleteGender(id);
        return ResponseEntity.noContent().build();
    }
}
