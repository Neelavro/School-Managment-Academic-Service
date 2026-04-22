package com.example.academic_service.controller;

import com.example.academic_service.dto.grading_dtos.*;
import com.example.academic_service.service.GradeService;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody GradeRequestDto dto) {
        try {
            GradeResponseDto data = gradeService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Grade created successfully",
                    "data", data
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        try {
            GradeResponseDto data = gradeService.getById(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Grade fetched successfully",
                    "data", data
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        try {
            List<GradeResponseDto> data = gradeService.getAll();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Grades fetched successfully",
                    "data", data
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                      @RequestBody GradeRequestDto dto) {
        try {
            GradeResponseDto data = gradeService.update(id, dto);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Grade updated successfully",
                    "data", data
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        try {
            gradeService.delete(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Grade deleted successfully"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}