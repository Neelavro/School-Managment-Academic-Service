package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.ExamAttendanceService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Working days + students' present days for the progress report (user, 2026-11-02).
 * GET lists the students the progress report would print (same filters) with their present days;
 * PUT saves the routine's working days and the present days typed in.
 */
@RestController
@RequestMapping("/api/exam-routines/{routineId}/attendance")
@RequiredArgsConstructor
public class ExamAttendanceController {

    private final ExamAttendanceService service;

    @GetMapping
    @RequirePermission(submodule = Submodule.PROGRESS_REPORT, action = "READ")
    public ResponseEntity<ApiResponse> sheet(@PathVariable Integer routineId,
                                             @RequestParam Integer classId,
                                             @RequestParam(required = false) Integer shiftId,
                                             @RequestParam(required = false) Integer genderSectionId,
                                             @RequestParam(required = false) Long sectionId,
                                             @RequestParam(required = false) Integer groupId,
                                             @RequestParam(required = false) Integer startRoll,
                                             @RequestParam(required = false) Integer endRoll) {
        return ResponseEntity.ok(new ApiResponse("OK",
                service.sheet(routineId, classId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll)));
    }

    /** {@code setWorkingDays: true} saves {@code workingDays} (null clears it); entries with presentDays null are removed. */
    public record SaveRequest(Integer workingDays, Boolean setWorkingDays, List<ExamAttendanceService.Entry> entries) {}

    @PutMapping
    @RequirePermission(submodule = Submodule.PROGRESS_REPORT, action = "UPDATE")
    public ResponseEntity<ApiResponse> save(@PathVariable Integer routineId, @RequestBody SaveRequest body) {
        int changed = service.save(routineId, Boolean.TRUE.equals(body.setWorkingDays()), body.workingDays(), body.entries());
        return ResponseEntity.ok(new ApiResponse("Attendance saved", Map.of("changed", changed)));
    }
}
