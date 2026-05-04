package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.exam_dtos.ExamClassRoomAssignmentRequestDto;
import com.example.academic_service.dto.exam_dtos.ExamClassRoomAssignmentResponseDto;
import com.example.academic_service.service.ExamClassRoomAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam-class-rooms")
@RequiredArgsConstructor
public class ExamClassRoomAssignmentController {

    private final ExamClassRoomAssignmentService service;

    @PostMapping
    public ResponseEntity<ApiResponse<List<ExamClassRoomAssignmentResponseDto>>> assign(
            @Valid @RequestBody ExamClassRoomAssignmentRequestDto dto) {
        return ResponseEntity.ok(service.assign(dto));
    }

    @GetMapping("/routine/{routineId}")
    public ResponseEntity<ApiResponse<List<ExamClassRoomAssignmentResponseDto>>> getByRoutine(
            @PathVariable Integer routineId) {
        return ResponseEntity.ok(service.getByRoutine(routineId));
    }

    @DeleteMapping("/routine/{routineId}/class/{classId}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @PathVariable Integer routineId,
            @PathVariable Integer classId) {
        return ResponseEntity.ok(service.removeByClassAndRoutine(routineId, classId));
    }
}
