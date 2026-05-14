package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.result_dtos.StudentRoutineResultResponse;
import com.example.academic_service.dto.student_portal_dtos.StudentPortalProfileDto;
import com.example.academic_service.dto.student_portal_dtos.UpcomingExamDto;
import com.example.academic_service.service.StudentPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student-portal")
@RequiredArgsConstructor
public class StudentPortalController {

    private final StudentPortalService service;

    private String resolveStudentSystemId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        Object raw = details != null ? details.get("studentSystemId") : null;
        if (raw == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a student account");
        return raw.toString();
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<StudentPortalProfileDto>> getProfile() {
        return ResponseEntity.ok(service.getProfile(resolveStudentSystemId()));
    }

    @GetMapping("/weekly-schedule")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWeeklySchedule() {
        return ResponseEntity.ok(service.getWeeklySchedule(resolveStudentSystemId()));
    }

    @GetMapping("/upcoming-exams")
    public ResponseEntity<ApiResponse<List<UpcomingExamDto>>> getUpcomingExams() {
        return ResponseEntity.ok(service.getUpcomingExams(resolveStudentSystemId()));
    }

    @GetMapping("/routines")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAvailableRoutines() {
        return ResponseEntity.ok(service.getAvailableRoutines(resolveStudentSystemId()));
    }

    @GetMapping("/result")
    public ResponseEntity<ApiResponse<StudentRoutineResultResponse>> getMyResult(
            @RequestParam Integer examRoutineId) {
        return ResponseEntity.ok(service.getMyResult(resolveStudentSystemId(), examRoutineId));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.changePassword(
                resolveStudentSystemId(),
                body.get("currentPassword"),
                body.get("newPassword")));
    }
}
