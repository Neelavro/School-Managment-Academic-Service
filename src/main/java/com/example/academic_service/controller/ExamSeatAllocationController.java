package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.AvailableRoomResponseDto;
import com.example.academic_service.dto.ExamSeatAllocationRequestDto;
import com.example.academic_service.dto.ExamSeatAllocationResponseDto;
import com.example.academic_service.service.impl.ExamSeatAllocationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/exam-seat-allocations")
@RequiredArgsConstructor
public class ExamSeatAllocationController {

    private final ExamSeatAllocationServiceImpl allocationService;

    @PostMapping
    public ApiResponse<ExamSeatAllocationResponseDto> create(
            @RequestBody ExamSeatAllocationRequestDto dto) {
        return allocationService.create(dto);
    }

    @PutMapping("/{id}")
    public ApiResponse<ExamSeatAllocationResponseDto> update(
            @PathVariable Integer id,
            @RequestBody ExamSeatAllocationRequestDto dto) {
        return allocationService.update(id, dto);
    }

    @GetMapping("/session/{sessionId}")
    public ApiResponse<List<ExamSeatAllocationResponseDto>> getBySession(
            @PathVariable Integer sessionId) {
        return allocationService.getBySession(sessionId);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        return allocationService.delete(id);
    }

    @GetMapping("/available-rooms")
    public ApiResponse<List<AvailableRoomResponseDto>> getAvailableRooms(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam Integer startRoll,
            @RequestParam Integer endRoll) {
        return allocationService.getAvailableRooms(date, startTime, endTime, startRoll, endRoll);
    }
}