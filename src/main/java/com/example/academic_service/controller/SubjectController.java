package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.SubjectRequestDto;
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