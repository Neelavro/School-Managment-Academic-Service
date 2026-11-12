package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.SubjectRequestDto;
import com.example.academic_service.dto.SubSubjectBulkRequest;
import com.example.academic_service.dto.SubSubjectClassesRequest;
import com.example.academic_service.entity.SubSubjectClass;
import com.example.academic_service.entity.Subject;
import com.example.academic_service.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping
    public ResponseEntity<ApiResponse<Subject>> create(
            @Valid @RequestBody SubjectRequestDto dto) {
        return ResponseEntity.ok(subjectService.create(dto));
    }

    /** Several sub-subjects of one parent at once, optionally assigned to classes. */
    @PostMapping("/sub-subjects")
    public ResponseEntity<ApiResponse<List<Subject>>> createSubSubjects(@RequestBody SubSubjectBulkRequest request) {
        return ResponseEntity.ok(subjectService.createSubSubjects(request));
    }

    /** Every sub-subject → class assignment. */
    @GetMapping("/sub-subject-classes")
    public ResponseEntity<ApiResponse<List<SubSubjectClass>>> subSubjectClasses() {
        return ResponseEntity.ok(subjectService.subSubjectClasses());
    }

    /** Assign sub-subjects to classes: adds by default; replace=true sets exactly these classes for each sub-subject. */
    @PutMapping("/sub-subject-classes")
    public ResponseEntity<ApiResponse<List<SubSubjectClass>>> assignSubSubjectClasses(@RequestBody SubSubjectClassesRequest request) {
        return ResponseEntity.ok(subjectService.assignSubSubjectClasses(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Subject>> update(
            @PathVariable Integer id,
            @Valid @RequestBody SubjectRequestDto dto) {
        return ResponseEntity.ok(subjectService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Subject>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(subjectService.getById(id));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Subject>>> getAll(
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(subjectService.getAll(active));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<Subject>> reactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(subjectService.reactivate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(subjectService.delete(id));
    }
}