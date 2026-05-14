package com.example.academic_service.controller;

import com.example.academic_service.dto.marking_dtos.SaveMarksRequest;
import com.example.academic_service.service.StudentMarkService;
import com.example.academic_service.service.TeacherDutyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/teacher-marks")
@RequiredArgsConstructor
public class TeacherMarkController {

    private final StudentMarkService studentMarkService;
    private final TeacherDutyService teacherDutyService;

    private Long resolveStaffId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        Object raw = details != null ? details.get("staffId") : null;
        if (raw == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No staff record linked to this account");
        return ((Number) raw).longValue();
    }

    private void validateAccess(Long staffId, Integer classId, Integer subjectId) {
        if (!teacherDutyService.hasMarkAccess(staffId, classId, subjectId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not assigned to teach this subject for this class");
        }
    }

    @GetMapping("/sheet")
    public ResponseEntity<Map<String, Object>> getMarkSheet(
            @RequestParam Integer routineId,
            @RequestParam Integer subjectId,
            @RequestParam Integer classId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer groupId) {
        Long staffId = resolveStaffId();
        validateAccess(staffId, classId, subjectId);
        return ResponseEntity.ok(studentMarkService.getMarkSheet(routineId, subjectId, classId, genderSectionId, sectionId, groupId));
    }

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveMarks(@RequestBody SaveMarksRequest request) {
        Long staffId = resolveStaffId();
        validateAccess(staffId, request.getClassId(), request.getSubjectId());
        return ResponseEntity.ok(studentMarkService.saveMarks(request));
    }
}
