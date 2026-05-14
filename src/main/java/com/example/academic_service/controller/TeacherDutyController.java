package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.duty_dtos.*;
import com.example.academic_service.dto.schedule_dtos.ClassRoutineResponseDto;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.TeacherDutyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher-duties")
@RequiredArgsConstructor
public class TeacherDutyController {

    private final TeacherDutyService service;

    @GetMapping("/teachers")
    @RequirePermission(submodule = Submodule.HR_TEACHER_DUTY, action = "READ")
    public ResponseEntity<ApiResponse<List<?>>> getTeachingStaff() {
        return ResponseEntity.ok(service.getTeachingStaff());
    }

    @GetMapping("/class-routines")
    @RequirePermission(submodule = Submodule.HR_TEACHER_DUTY, action = "READ")
    public ResponseEntity<ApiResponse<List<ClassRoutineResponseDto>>> getActiveClassRoutines() {
        return ResponseEntity.ok(service.getActiveClassRoutines());
    }

    @GetMapping("/exam-sessions")
    @RequirePermission(submodule = Submodule.HR_TEACHER_DUTY, action = "READ")
    public ResponseEntity<ApiResponse<List<ExamSessionForDutyDto>>> getAvailableExamSessions() {
        return ResponseEntity.ok(service.getAvailableExamSessions());
    }

    @GetMapping("/exam-sessions/{examSessionId}/available-rooms")
    @RequirePermission(submodule = Submodule.HR_TEACHER_DUTY, action = "READ")
    public ResponseEntity<ApiResponse<List<AvailableRoomDto>>> getAvailableRooms(@PathVariable Integer examSessionId) {
        return ResponseEntity.ok(service.getAvailableRoomsForExamSession(examSessionId));
    }

    @GetMapping("/staff/{staffId}/periods")
    @RequirePermission(submodule = Submodule.HR_TEACHER_DUTY, action = "READ")
    public ResponseEntity<ApiResponse<List<TeacherPeriodDutyResponseDto>>> getPeriodDuties(@PathVariable Long staffId) {
        return ResponseEntity.ok(service.getPeriodDuties(staffId));
    }

    @GetMapping("/staff/{staffId}/exams")
    @RequirePermission(submodule = Submodule.HR_TEACHER_DUTY, action = "READ")
    public ResponseEntity<ApiResponse<List<TeacherExamDutyResponseDto>>> getExamDuties(@PathVariable Long staffId) {
        return ResponseEntity.ok(service.getExamDuties(staffId));
    }

    @PostMapping("/staff/{staffId}/periods")
    @RequirePermission(submodule = Submodule.HR_TEACHER_DUTY, action = "CREATE")
    public ResponseEntity<ApiResponse<List<TeacherPeriodDutyResponseDto>>> assignPeriodDuties(
            @PathVariable Long staffId,
            @Valid @RequestBody TeacherPeriodDutyBulkRequestDto dto) {
        return ResponseEntity.ok(service.assignPeriodDuties(staffId, dto));
    }

    @PostMapping("/staff/{staffId}/exams")
    @RequirePermission(submodule = Submodule.HR_TEACHER_DUTY, action = "CREATE")
    public ResponseEntity<ApiResponse<List<TeacherExamDutyResponseDto>>> assignExamDuties(
            @PathVariable Long staffId,
            @Valid @RequestBody TeacherExamDutyBulkRequestDto dto) {
        return ResponseEntity.ok(service.assignExamDuties(staffId, dto));
    }

    @DeleteMapping("/periods/{id}")
    @RequirePermission(submodule = Submodule.HR_TEACHER_DUTY, action = "DELETE")
    public ResponseEntity<ApiResponse<Void>> removePeriodDuty(@PathVariable Long id) {
        return ResponseEntity.ok(service.removePeriodDuty(id));
    }

    @DeleteMapping("/exams/{id}")
    @RequirePermission(submodule = Submodule.HR_TEACHER_DUTY, action = "DELETE")
    public ResponseEntity<ApiResponse<Void>> removeExamDuty(@PathVariable Long id) {
        return ResponseEntity.ok(service.removeExamDuty(id));
    }
}
