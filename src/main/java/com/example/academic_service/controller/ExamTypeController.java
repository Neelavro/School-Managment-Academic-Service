package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.exam_dtos.ExamTypeRequestDto;
import com.example.academic_service.entity.ExamType;
import com.example.academic_service.service.ExamTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam-types")
@RequiredArgsConstructor
public class ExamTypeController {

    private final ExamTypeService examTypeService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExamType>> create(@Valid @RequestBody ExamTypeRequestDto dto) {
        return ResponseEntity.ok(examTypeService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExamType>> update(@PathVariable Integer id,
                                                        @Valid @RequestBody ExamTypeRequestDto dto) {
        return ResponseEntity.ok(examTypeService.update(id, dto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExamType>>> getAll(
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(examTypeService.getAll(active));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<ExamType>> reactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(examTypeService.reactivate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(examTypeService.delete(id));
    }
}