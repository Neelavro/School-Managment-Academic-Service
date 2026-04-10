package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.ClassSubjectGroupRequestDto;
import com.example.academic_service.entity.ClassSubjectGroup;
import com.example.academic_service.service.ClassSubjectGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/class-subject-groups")
@RequiredArgsConstructor
public class ClassSubjectGroupController {

    private final ClassSubjectGroupService service;

    @PostMapping
    public ResponseEntity<ApiResponse<ClassSubjectGroup>> assign(
            @Valid @RequestBody ClassSubjectGroupRequestDto dto) {
        return ResponseEntity.ok(service.assign(dto));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<List<ClassSubjectGroup>>> getByClass(
            @PathVariable Integer classId) {
        return ResponseEntity.ok(service.getByClass(classId));
    }
    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<ApiResponse<List<ClassSubjectGroup>>> getBySubject(
            @PathVariable Integer subjectId) {
        return ResponseEntity.ok(service.getBySubject(subjectId));
    }

    @GetMapping("/class/{classId}/group/{studentGroupId}")
    public ResponseEntity<ApiResponse<List<ClassSubjectGroup>>> getByClassAndGroup(
            @PathVariable Integer classId,
            @PathVariable Integer studentGroupId) {
        return ResponseEntity.ok(service.getByClassAndGroup(classId, studentGroupId));
    }

    @GetMapping("/class/{classId}/no-group")
    public ResponseEntity<ApiResponse<List<ClassSubjectGroup>>> getByClassNoGroup(
            @PathVariable Integer classId) {
        return ResponseEntity.ok(service.getByClassAndGroup(classId, null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable Integer id) {
        return ResponseEntity.ok(service.remove(id));
    }
}