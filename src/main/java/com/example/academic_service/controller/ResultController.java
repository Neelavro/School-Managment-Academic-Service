package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.service.ResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;

    // ─── RESULTS ─────────────────────────────────────────────────────────────────

    @GetMapping("/session")
    public ResponseEntity<?> getSessionResult(
            @RequestParam Integer examSessionId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer groupId) {
        return ResponseEntity.ok(ApiResponse.success("Session result fetched",
                resultService.getSessionResult(examSessionId, genderSectionId, sectionId, groupId)));
    }

    @GetMapping("/routine")
    public ResponseEntity<?> getRoutineResult(
            @RequestParam Integer examRoutineId,
            @RequestParam Integer classId,
            @RequestParam(required = false) Integer shiftId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) Integer startRoll,
            @RequestParam(required = false) Integer endRoll) {
        return ResponseEntity.ok(ApiResponse.success("Routine result fetched",
                resultService.getRoutineResult(examRoutineId, classId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll)));
    }

    @GetMapping("/annual")
    public ResponseEntity<?> getAnnualResult(
            @RequestParam Integer academicYearId,
            @RequestParam Integer classId,
            @RequestParam(required = false) Integer shiftId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) Integer startRoll,
            @RequestParam(required = false) Integer endRoll) {
        return ResponseEntity.ok(ApiResponse.success("Annual result fetched",
                resultService.getAnnualResult(academicYearId, classId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll)));
    }

    @GetMapping("/student/routine")
    public ResponseEntity<?> getStudentRoutineResult(
            @RequestParam Long enrollmentId,
            @RequestParam Integer examRoutineId) {
        return ResponseEntity.ok(ApiResponse.success("Student routine result fetched",
                resultService.getStudentRoutineResult(enrollmentId, examRoutineId)));
    }

    @GetMapping("/student/annual")
    public ResponseEntity<?> getStudentAnnualResult(
            @RequestParam Long enrollmentId,
            @RequestParam Integer academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("Student annual result fetched",
                resultService.getStudentAnnualResult(enrollmentId, academicYearId)));
    }

    @GetMapping("/merit-list")
    public ResponseEntity<?> getMeritList(
            @RequestParam Integer academicYearId,
            @RequestParam Integer classId,
            @RequestParam(required = false) Integer shiftId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) Integer startRoll,
            @RequestParam(required = false) Integer endRoll) {
        return ResponseEntity.ok(ApiResponse.success("Merit list fetched",
                resultService.getMeritList(academicYearId, classId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll)));
    }

    // ─── STATS ───────────────────────────────────────────────────────────────────

    @GetMapping("/stats/session")
    public ResponseEntity<?> getSessionStats(
            @RequestParam Integer examSessionId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer groupId) {
        return ResponseEntity.ok(ApiResponse.success("Session stats fetched",
                resultService.getSessionStats(examSessionId, genderSectionId, sectionId, groupId)));
    }

    @GetMapping("/overview/routine")
    public ResponseEntity<?> getRoutineOverview(
            @RequestParam Integer examRoutineId,
            @RequestParam Integer classId,
            @RequestParam(required = false) Integer shiftId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) Integer startRoll,
            @RequestParam(required = false) Integer endRoll) {
        return ResponseEntity.ok(ApiResponse.success("Routine overview fetched",
                resultService.getRoutineOverview(examRoutineId, classId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll)));
    }

    @GetMapping("/overview/annual")
    public ResponseEntity<?> getAnnualOverview(
            @RequestParam Integer academicYearId,
            @RequestParam Integer classId,
            @RequestParam(required = false) Integer shiftId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) Integer startRoll,
            @RequestParam(required = false) Integer endRoll) {
        return ResponseEntity.ok(ApiResponse.success("Annual overview fetched",
                resultService.getAnnualOverview(academicYearId, classId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll)));
    }

    @GetMapping("/stats/routine")
    public ResponseEntity<?> getRoutineStats(
            @RequestParam Integer examRoutineId,
            @RequestParam Integer classId,
            @RequestParam(required = false) Integer shiftId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) Integer startRoll,
            @RequestParam(required = false) Integer endRoll) {
        return ResponseEntity.ok(ApiResponse.success("Routine stats fetched",
                resultService.getRoutineStats(examRoutineId, classId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll)));
    }

    @GetMapping("/stats/annual")
    public ResponseEntity<?> getAnnualStats(
            @RequestParam Integer academicYearId,
            @RequestParam Integer classId,
            @RequestParam(required = false) Integer shiftId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) Integer startRoll,
            @RequestParam(required = false) Integer endRoll) {
        return ResponseEntity.ok(ApiResponse.success("Annual stats fetched",
                resultService.getAnnualStats(academicYearId, classId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll)));
    }
}
