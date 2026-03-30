package com.example.academic_service.controller;

import com.example.academic_service.entity.GenderSection;
import com.example.academic_service.entity.Section;
import com.example.academic_service.service.GenderSectionService;
import com.example.academic_service.service.SectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sections")
public class SectionController {

    private final SectionService sectionService;
    private final GenderSectionService genderSectionService;

    public SectionController(SectionService sectionService, GenderSectionService genderSectionService) {
        this.sectionService = sectionService;
        this.genderSectionService = genderSectionService;
    }

    @PostMapping("/migrate")
    public ResponseEntity<?> migrate(@RequestBody Section request) {
        sectionService.migrateSection(request);
        return ResponseEntity.ok("Migrated");
    }
    @PostMapping("/migrate-gender-section")
    public ResponseEntity<?> migrate(@RequestBody GenderSection request) {
        genderSectionService.migrateGenderSection(request);
        return ResponseEntity.ok("Migrated");
    }
    @PostMapping
    public ResponseEntity<Section> create(@RequestBody Section section) {
        if (section.getClassEntity() == null || section.getClassEntity().getId() == null) {
            return ResponseEntity
                    .badRequest()
                    .body(null);  // Or throw a proper exception
        }
        return new ResponseEntity<>(
                sectionService.createSection(section),
                HttpStatus.CREATED
        );
    }
    @GetMapping("/class/{classId}/gender/{genderSectionId}")
    public ResponseEntity<List<Section>> getAllByClass(
            @PathVariable Integer classId,
            @PathVariable Integer genderSectionId) {
        List<Section> sections = sectionService.getSectionsByClassIdAndGenderSectionId(classId, genderSectionId);
        System.out.println(sections);
        return ResponseEntity.ok(sections);
    }

    @GetMapping("/gender-sections")
    public ResponseEntity<List<GenderSection>> getAllGenderSections() {
        List<GenderSection> genderSections = genderSectionService.getAllGenderSections();
        return ResponseEntity.ok(genderSections);
    }



    @PutMapping("/{id}")
    public ResponseEntity<Section> update(
            @PathVariable Long id,
            @RequestBody Section section
    ) {
        return ResponseEntity.ok(
                sectionService.updateSection(id, section)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Section> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                sectionService.getSectionById(id)
        );
    }

    @GetMapping
    public ResponseEntity<List<Section>> getAll() {
        return ResponseEntity.ok(
                sectionService.getAllActiveSections()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sectionService.deleteSection(id);
        return ResponseEntity.noContent().build();
    }
}
