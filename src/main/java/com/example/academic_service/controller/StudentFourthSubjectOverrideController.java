package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.StudentFourthSubjectOverrideDto;
import com.example.academic_service.service.StudentFourthSubjectOverrideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student-fourth-subject-overrides")
@RequiredArgsConstructor
public class StudentFourthSubjectOverrideController {

    private final StudentFourthSubjectOverrideService service;

    @PostMapping
    public ResponseEntity<ApiResponse<StudentFourthSubjectOverrideDto.Response>> setOverride(
            @RequestBody StudentFourthSubjectOverrideDto.Request request) {
        return ResponseEntity.ok(service.setOverride(request));
    }

    @GetMapping("/enrollment/{enrollmentId}")
    public ResponseEntity<ApiResponse<StudentFourthSubjectOverrideDto.Response>> getByEnrollment(
            @PathVariable Long enrollmentId) {
        return ResponseEntity.ok(service.getByEnrollment(enrollmentId));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<List<StudentFourthSubjectOverrideDto.Response>>> getByClass(
            @PathVariable Integer classId) {
        return ResponseEntity.ok(service.getByClass(classId));
    }

    @DeleteMapping("/enrollment/{enrollmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteOverride(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(service.deleteOverride(enrollmentId));
    }
}
