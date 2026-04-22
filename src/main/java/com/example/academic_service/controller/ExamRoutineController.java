package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.exam_dtos.ExamRoutineRequestDto;
import com.example.academic_service.entity.ExamRoutine;
import com.example.academic_service.service.ExamRoutineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam-routines")
@RequiredArgsConstructor
public class ExamRoutineController {

    private final ExamRoutineService examRoutineService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExamRoutine>> create(@Valid @RequestBody ExamRoutineRequestDto dto) {
        return ResponseEntity.ok(examRoutineService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExamRoutine>> update(@PathVariable Integer id,
                                                           @Valid @RequestBody ExamRoutineRequestDto dto) {
        return ResponseEntity.ok(examRoutineService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExamRoutine>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(examRoutineService.getById(id));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExamRoutine>>> getAll(
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(examRoutineService.getAll(active));
    }

    @GetMapping("/by-academic-year/{academicYearId}")
    public ResponseEntity<ApiResponse<List<ExamRoutine>>> getByAcademicYear(
            @PathVariable Integer academicYearId) {
        return ResponseEntity.ok(examRoutineService.getByAcademicYear(academicYearId));
    }

    @GetMapping("/by-exam-type/{examTypeId}")
    public ResponseEntity<ApiResponse<List<ExamRoutine>>> getByExamType(
            @PathVariable Integer examTypeId) {
        return ResponseEntity.ok(examRoutineService.getByExamType(examTypeId));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<ExamRoutine>> publish(@PathVariable Integer id) {
        return ResponseEntity.ok(examRoutineService.publish(id));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<ExamRoutine>> reactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(examRoutineService.reactivate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(examRoutineService.delete(id));
    }

    @PatchMapping("/{id}/unpublish")
    public ResponseEntity<ApiResponse<ExamRoutine>> unpublish(@PathVariable Integer id) {
        return ResponseEntity.ok(examRoutineService.unpublish(id));
    }
}