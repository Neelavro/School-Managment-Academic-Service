package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.schedule_dtos.AcademicCalendarEntryRequestDto;
import com.example.academic_service.entity.AcademicCalendarEntry;
import com.example.academic_service.entity.CalendarEntryType;
import com.example.academic_service.service.AcademicCalendarEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-calendar")
@RequiredArgsConstructor
public class AcademicCalendarEntryController {

    private final AcademicCalendarEntryService service;

    @PostMapping
    public ResponseEntity<ApiResponse<AcademicCalendarEntry>> create(
            @Valid @RequestBody AcademicCalendarEntryRequestDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AcademicCalendarEntry>> update(
            @PathVariable Integer id,
            @Valid @RequestBody AcademicCalendarEntryRequestDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AcademicCalendarEntry>>> getByAcademicYear(
            @RequestParam Integer academicYearId,
            @RequestParam(required = false) CalendarEntryType type) {
        return ResponseEntity.ok(service.getByAcademicYear(academicYearId, type));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(service.delete(id));
    }
}
