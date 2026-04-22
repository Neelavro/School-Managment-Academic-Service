package com.example.academic_service.controller;

import com.example.academic_service.dto.admit_card_dtos.AdmitCardRoutineResponseDto;
import com.example.academic_service.service.impl.AdmitCardServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admit-card")
@RequiredArgsConstructor
public class AdmitCardController {

    private final AdmitCardServiceImpl admitCardService;

    @GetMapping
    public ResponseEntity<AdmitCardRoutineResponseDto> getAdmitCardData(
            @RequestParam Integer routineId,
            @RequestParam(required = false) Integer sessionId,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Long sectionId
    ) {
        return ResponseEntity.ok(
                admitCardService.getAdmitCardData(
                        routineId, sessionId, classId, genderSectionId, sectionId)
        );
    }
    @GetMapping("/by-section")
    public ResponseEntity<AdmitCardRoutineResponseDto> getAdmitCardDataBySection(
            @RequestParam Integer routineId,
            @RequestParam(required = false) Integer sessionId,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer groupId        // ← add
    ) {
        return ResponseEntity.ok(
                admitCardService.getAdmitCardDataBySection(
                        routineId, sessionId, classId, genderSectionId, sectionId, groupId)  // ← pass
        );
    }
}