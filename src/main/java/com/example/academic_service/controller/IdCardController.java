package com.example.academic_service.controller;

import com.example.academic_service.dto.EnrollmentResponseDto;
import com.example.academic_service.service.IdCardPdfService;
import com.example.academic_service.service.SeatPlanPdfService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/id-card")
@RequiredArgsConstructor
public class IdCardController {

    private final IdCardPdfService idCardPdfService;
    private final SeatPlanPdfService seatPlanPdfService;

    @GetMapping("/download")
    public void downloadIdCards(
            @RequestParam(required = false) Integer academicYearId,
            @RequestParam(required = false) Integer shiftId,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(required = false) Integer startRoll,
            @RequestParam(required = false) Integer endRoll,
            HttpServletResponse response
    ) throws Exception {

        List<EnrollmentResponseDto> enrollments = seatPlanPdfService.fetchEnrollments(
                academicYearId, classId, sectionId, shiftId, genderSectionId,
                groupId, startRoll, endRoll);

        enrollments.sort(Comparator.comparingInt(
                e -> e.getClassRoll() != null ? e.getClassRoll() : 0));

        byte[] pdfBytes = idCardPdfService.generatePdf(enrollments);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=student_id_cards.pdf");
        response.getOutputStream().write(pdfBytes);
        response.getOutputStream().flush();
    }

    @GetMapping("/download-back")
    public void downloadIdCardBack(HttpServletResponse response) throws Exception {
        byte[] pdfBytes = idCardPdfService.generateBackPdf();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=student_id_card_back.pdf");
        response.getOutputStream().write(pdfBytes);
        response.getOutputStream().flush();
    }
}
