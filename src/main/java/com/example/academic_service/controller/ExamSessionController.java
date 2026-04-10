package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.AvailableRoomResponseDto;
import com.example.academic_service.dto.ExamSessionRequestDto;
import com.example.academic_service.dto.ExamSessionResponseDto;
import com.example.academic_service.entity.ExamSession;
import com.example.academic_service.service.ExamSessionService;
import com.example.academic_service.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/exam-sessions")
@RequiredArgsConstructor
public class ExamSessionController {

    private final ExamSessionService examSessionService;
    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExamSessionResponseDto>> create(@Valid @RequestBody ExamSessionRequestDto dto) {
        return ResponseEntity.ok(examSessionService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExamSessionResponseDto>> update(@PathVariable Integer id,
                                                                      @Valid @RequestBody ExamSessionRequestDto dto) {
        return ResponseEntity.ok(examSessionService.update(id, dto));
    }

    @GetMapping("/routine/{routineId}")
    public ResponseEntity<ApiResponse<List<ExamSessionResponseDto>>> getByRoutine(
            @PathVariable Integer routineId,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(examSessionService.getByRoutine(routineId, active));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<ExamSessionResponseDto>> reactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(examSessionService.reactivate(id));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(examSessionService.delete(id));
    }
}
