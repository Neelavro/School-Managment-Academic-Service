package com.example.academic_service.controller;

import com.example.academic_service.dto.marking_dtos.SaveMarksRequest;
import com.example.academic_service.service.StudentMarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/student-marks")
@RequiredArgsConstructor
public class StudentMarkController {

    private final StudentMarkService studentMarkService;

    @GetMapping("/sheet")
    public ResponseEntity<Map<String, Object>> getMarkSheet(
            @RequestParam Integer examSessionId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer groupId) {
        return ResponseEntity.ok(
                studentMarkService.getMarkSheet(examSessionId, genderSectionId, sectionId, groupId));
    }

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveMarks(@RequestBody SaveMarksRequest request) {
        return ResponseEntity.ok(studentMarkService.saveMarks(request));
    }
}