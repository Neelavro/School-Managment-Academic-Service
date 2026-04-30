package com.example.academic_service.controller;

import com.example.academic_service.service.ProgressReportPdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progress-report")
@RequiredArgsConstructor
@Slf4j
public class ProgressReportController {

    private final ProgressReportPdfService pdfService;

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(
            @RequestParam Integer routineId,
            @RequestParam Integer classId,
            @RequestParam(required = false) Integer shiftId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) Integer startRoll,
            @RequestParam(required = false) Integer endRoll
    ) {
        try {
            byte[] pdf = pdfService.generate(
                    routineId, classId, shiftId, genderSectionId,
                    sectionId, groupId, startRoll, endRoll);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"progress-report.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate progress report", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
