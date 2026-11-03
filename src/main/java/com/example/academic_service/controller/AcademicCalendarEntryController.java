package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.schedule_dtos.AcademicCalendarEntryRequestDto;
import com.example.academic_service.entity.AcademicCalendarEntry;
import com.example.academic_service.entity.CalendarEntryType;
import com.example.academic_service.service.AcademicCalendarEntryService;
import com.example.academic_service.service.SchoolCalendarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/academic-calendar")
@RequiredArgsConstructor
public class AcademicCalendarEntryController {

    private final AcademicCalendarEntryService service;
    private final SchoolCalendarService schoolCalendarService;

    public record CopyRequest(Integer fromAcademicYearId, Integer toAcademicYearId) {}

    /** Copies a year's Exam Days into another year, one year later (user, 2026-11-03). Returns {copied[], skipped[]}. */
    @PostMapping("/copy-exam-days")
    public ResponseEntity<ApiResponse<Map<String, Object>>> copyExamDays(@RequestBody CopyRequest body) {
        return ResponseEntity.ok(ApiResponse.success("Exam days copied", service.copyExamDays(body.fromAcademicYearId(), body.toAcademicYearId())));
    }

    public record WeekendsRequest(List<String> days) {}

    /** The school's weekly off days, e.g. ["FRIDAY"]. */
    @GetMapping("/weekends")
    public ResponseEntity<ApiResponse<List<DayOfWeek>>> weekends() {
        return ResponseEntity.ok(ApiResponse.success("Weekends", schoolCalendarService.weekends()));
    }

    @PutMapping("/weekends")
    public ResponseEntity<ApiResponse<List<DayOfWeek>>> setWeekends(@RequestBody WeekendsRequest body) {
        return ResponseEntity.ok(ApiResponse.success("Weekends saved", schoolCalendarService.setWeekends(body.days())));
    }

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
