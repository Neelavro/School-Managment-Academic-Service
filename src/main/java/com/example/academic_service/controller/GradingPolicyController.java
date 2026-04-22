package com.example.academic_service.controller;

import com.example.academic_service.dto.grading_dtos.GradingPolicyRequestDto;
import com.example.academic_service.dto.grading_dtos.GradingPolicyResponseDto;
import com.example.academic_service.service.GradingPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grading-policies")
@RequiredArgsConstructor
public class GradingPolicyController {

    private final GradingPolicyService gradingPolicyService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody GradingPolicyRequestDto dto) {
        try {
            GradingPolicyResponseDto data = gradingPolicyService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Grading policy created successfully",
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
            GradingPolicyResponseDto data = gradingPolicyService.getById(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Grading policy fetched successfully",
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
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(required = false) Boolean isActive) {
        try {
            List<GradingPolicyResponseDto> data = isActive != null
                    ? gradingPolicyService.getByActive(isActive)
                    : gradingPolicyService.getAll();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Grading policies fetched successfully",
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
                                                      @RequestBody GradingPolicyRequestDto dto) {
        try {
            GradingPolicyResponseDto data = gradingPolicyService.update(id, dto);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Grading policy updated successfully",
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
            gradingPolicyService.delete(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Grading policy deleted successfully"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}