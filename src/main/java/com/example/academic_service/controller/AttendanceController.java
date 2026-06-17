package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.AttendanceService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// @RestController — disabled on production-full-system
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    // ── Admin endpoints ───────────────────────────────────────────────────────

    @GetMapping("/students")
    @RequirePermission(submodule = Submodule.ATTENDANCE, action = "READ")
    public ResponseEntity<ApiResponse> getStudents(
            @RequestParam Long sectionId,
            @RequestParam Integer academicYearId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(new ApiResponse("OK",
                attendanceService.getStudentsWithStatus(sectionId, academicYearId, date)));
    }

    @PostMapping("/save")
    @RequirePermission(submodule = Submodule.ATTENDANCE, action = "CREATE")
    public ResponseEntity<ApiResponse> save(@RequestBody Map<String, Object> body) {
        Long sectionId = ((Number) body.get("sectionId")).longValue();
        Integer academicYearId = ((Number) body.get("academicYearId")).intValue();
        LocalDate date = LocalDate.parse((String) body.get("date"));
        @SuppressWarnings("unchecked")
        List<Long> absentIds = ((List<Number>) body.get("absentEnrollmentIds"))
                .stream().map(Number::longValue).toList();
        attendanceService.saveAttendance(sectionId, academicYearId, date, absentIds);
        return ResponseEntity.ok(new ApiResponse("Attendance saved", null));
    }

    // ── Teacher portal endpoints (no FBAC — reads staffId from JWT) ───────────

    @GetMapping("/me/students")
    public ResponseEntity<ApiResponse> getMyStudents(
            @RequestParam Integer academicYearId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long staffId = resolveStaffId();
        return ResponseEntity.ok(new ApiResponse("OK",
                attendanceService.getTeacherStudentsWithStatus(staffId, academicYearId, date)));
    }

    @PostMapping("/me/save")
    public ResponseEntity<ApiResponse> saveMyAttendance(@RequestBody Map<String, Object> body) {
        Long staffId = resolveStaffId();
        Integer academicYearId = ((Number) body.get("academicYearId")).intValue();
        LocalDate date = LocalDate.parse((String) body.get("date"));
        @SuppressWarnings("unchecked")
        List<Long> absentIds = ((List<Number>) body.get("absentEnrollmentIds"))
                .stream().map(Number::longValue).toList();
        attendanceService.saveTeacherAttendance(staffId, academicYearId, date, absentIds);
        return ResponseEntity.ok(new ApiResponse("Attendance saved", null));
    }

    private Long resolveStaffId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        Object raw = details != null ? details.get("staffId") : null;
        if (raw == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No staff record linked to this account");
        return ((Number) raw).longValue();
    }
}
