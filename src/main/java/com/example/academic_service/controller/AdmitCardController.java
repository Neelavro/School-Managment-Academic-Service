package com.example.academic_service.controller;

import com.example.academic_service.dto.admit_card_dtos.AdmitCardRoutineResponseDto;
import com.example.academic_service.service.AdmitCardPdfService;
import com.example.academic_service.service.impl.AdmitCardServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admit-card")
@RequiredArgsConstructor
@Slf4j
public class AdmitCardController {

    private final AdmitCardServiceImpl admitCardService;
    private final AdmitCardPdfService admitCardPdfService;

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
                        routineId, sessionId, classId, genderSectionId, sectionId, groupId)
        );
    }

    @GetMapping(value = "/download", produces = "application/pdf")
    public ResponseEntity<byte[]> downloadAdmitCards(
            @RequestParam Integer routineId,
            @RequestParam(required = false) Integer sessionId,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Long sectionId
    ) {
        try {
            byte[] pdf = admitCardPdfService.generateAdmitCards(
                    routineId, sessionId, classId, genderSectionId, sectionId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"admit-cards.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate admit cards", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/download/by-section", produces = "application/pdf")
    public ResponseEntity<byte[]> downloadAdmitCardsBySection(
            @RequestParam Integer routineId,
            @RequestParam(required = false) Integer sessionId,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) Integer startRoll,
            @RequestParam(required = false) Integer endRoll
    ) {
        try {
            byte[] pdf = admitCardPdfService.generateAdmitCardsBySection(
                    routineId, sessionId, classId, genderSectionId, sectionId,
                    groupId, startRoll, endRoll);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"admit-cards-by-section.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate admit cards by section", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}