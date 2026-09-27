package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.exam_dtos.CloneRoutineRequestDto;
import com.example.academic_service.dto.exam_dtos.ExamRoutineRequestDto;
import com.example.academic_service.entity.ExamRoutine;
import com.example.academic_service.service.ExamRoutineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @PostMapping("/{id}/clone")
    public ResponseEntity<ApiResponse<ExamRoutine>> clone(@PathVariable Integer id,
                                                          @Valid @RequestBody CloneRoutineRequestDto dto) {
        return ResponseEntity.ok(examRoutineService.clone(id, dto));
    }

    @PatchMapping("/{id}/publish-results")
    public ResponseEntity<ApiResponse<ExamRoutine>> publishResults(
            @PathVariable Integer id, @RequestParam Integer classId) {
        return ResponseEntity.ok(examRoutineService.publishResults(id, classId));
    }

    @PatchMapping("/{id}/unpublish-results")
    public ResponseEntity<ApiResponse<ExamRoutine>> unpublishResults(
            @PathVariable Integer id, @RequestParam Integer classId) {
        return ResponseEntity.ok(examRoutineService.unpublishResults(id, classId));
    }

    @GetMapping("/{id}/class-publication-status")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getClassPublicationStatus(
            @PathVariable Integer id) {
        return ResponseEntity.ok(examRoutineService.getClassPublicationStatus(id));
    }

    /** Recompute a published class's results and save the differences for review (background job). */
    @PostMapping("/{id}/result-changes/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkResultChanges(
            @PathVariable Integer id, @RequestParam Integer classId) {
        return ResponseEntity.ok(examRoutineService.checkResultChanges(id, classId));
    }

    /** One page of a finished check's changes: each student's old and new values. */
    @GetMapping("/{id}/result-changes/{jobId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getResultChanges(
            @PathVariable Integer id, @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(examRoutineService.getResultChanges(id, jobId, page, size));
    }

    /** Write the reviewed changes to the stored results; the class stays published. */
    @PostMapping("/{id}/result-changes/{jobId}/apply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyResultChanges(
            @PathVariable Integer id, @PathVariable Long jobId) {
        return ResponseEntity.ok(examRoutineService.applyResultChanges(id, jobId));
    }

    @DeleteMapping("/{id}/result-changes/{jobId}")
    public ResponseEntity<ApiResponse<Void>> discardResultChanges(
            @PathVariable Integer id, @PathVariable Long jobId) {
        return ResponseEntity.ok(examRoutineService.discardResultChanges(id, jobId));
    }
}