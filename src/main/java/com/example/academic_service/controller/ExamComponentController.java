package com.example.academic_service.controller;

import com.example.academic_service.dto.marking_dtos.*;
import com.example.academic_service.service.ExamComponentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exam-components")
@RequiredArgsConstructor
public class ExamComponentController {

    private final ExamComponentService examComponentService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody ExamComponentRequest request) {
        return ResponseEntity.ok(examComponentService.create(request));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        return ResponseEntity.ok(examComponentService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id, @RequestBody ExamComponentRequest request) {
        return ResponseEntity.ok(examComponentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(examComponentService.delete(id));
    }
}