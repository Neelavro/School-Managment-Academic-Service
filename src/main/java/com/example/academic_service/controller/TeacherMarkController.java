package com.example.academic_service.controller;

import com.example.academic_service.dto.marking_dtos.SaveMarksRequest;
import com.example.academic_service.repository.ResultPublicationRepository;
import com.example.academic_service.service.StudentMarkService;
import com.example.academic_service.service.TeacherSubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher-marks")
@RequiredArgsConstructor
public class TeacherMarkController {

    private final StudentMarkService studentMarkService;
    private final TeacherSubjectService teacherSubjectService;
    private final ResultPublicationRepository resultPublicationRepository;

    static final String PUBLISHED_LOCK_MESSAGE =
            "Results for this class are published, so marks can't be changed here. Ask the office to change them.";

    /** Teachers can't change marks once the class's result is published for the exam (user rule 2026-10-28). Admins still can. */
    private boolean resultPublished(Integer routineId, Integer classId) {
        return resultPublicationRepository.findByExamRoutine_IdAndStudentClass_Id(routineId, classId)
                .map(p -> Boolean.TRUE.equals(p.getPublished())).orElse(false);
    }

    private Long resolveStaffId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        Object raw = details != null ? details.get("staffId") : null;
        if (raw == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No staff record linked to this account");
        return ((Number) raw).longValue();
    }

    /** The teacher must be assigned the subject for the class in the exam routine's academic year. */
    private void validateAccess(Long staffId, Integer routineId, Integer classId, Integer subjectId) {
        if (!teacherSubjectService.hasMarkAccess(staffId, routineId, classId, subjectId)) {
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
        validateAccess(staffId, routineId, classId, subjectId);
        Map<String, Object> sheet = new LinkedHashMap<>(
                studentMarkService.getMarkSheet(routineId, subjectId, classId, genderSectionId, sectionId, groupId));
        boolean locked = resultPublished(routineId, classId);
        sheet.put("locked", locked);
        sheet.put("lockedReason", locked ? PUBLISHED_LOCK_MESSAGE : null);
        return ResponseEntity.ok(sheet);
    }

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveMarks(@RequestBody SaveMarksRequest request) {
        Long staffId = resolveStaffId();
        validateAccess(staffId, request.getRoutineId(), request.getClassId(), request.getSubjectId());
        if (resultPublished(request.getRoutineId(), request.getClassId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, PUBLISHED_LOCK_MESSAGE);
        SaveMarksRequest fresh = studentMarkService.onlyNewMarks(request);
        if (fresh.getMarks() == null || fresh.getMarks().isEmpty())
            return ResponseEntity.ok(Map.of("message", "No new marks to save", "data", java.util.List.of()));
        return ResponseEntity.ok(studentMarkService.saveMarks(fresh));
    }
}
