package com.example.academic_service.controller;

import com.example.academic_service.dto.EnrollmentResponseDto;
import com.example.academic_service.service.SeatPlanPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/seat-plan")
@RequiredArgsConstructor
public class SeatPlanController {

    private final SeatPlanPdfService seatPlanPdfService;

    @GetMapping("/generate")
    public ResponseEntity<byte[]> generateSeatPlan(
            @RequestParam String examName,
            @RequestParam(required = false) Integer academicYearId,
            @RequestParam(required = false) Integer shiftId,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) Integer startRoll,
            @RequestParam(required = false) Integer endRoll
    ) throws Exception {

        List<EnrollmentResponseDto> enrollments = seatPlanPdfService.fetchEnrollments(
                academicYearId, classId, sectionId, shiftId, genderSectionId,
                groupId, startRoll, endRoll);

        if (enrollments.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        enrollments.sort(Comparator.comparingInt(
                e -> e.getClassRoll() != null ? e.getClassRoll() : 0));

        byte[] pdf = seatPlanPdfService.generatePdf(enrollments, examName);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"seat-plan.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
