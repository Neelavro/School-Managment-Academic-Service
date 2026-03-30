package com.example.academic_service.controller;

import com.example.academic_service.dto.EnrollmentResponseDto;
import com.example.academic_service.entity.Enrollment;
import com.example.academic_service.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * GET /api/enrollments
     * Query params:
     *   academicYearId, classId, sectionId, shiftId,
     *   genderSectionId, studentGroupId, isActive,
     *   page, size, sort, direction
     */
    @GetMapping
    public ResponseEntity<Page<EnrollmentResponseDto>> getEnrollments(
            @RequestParam(required = false) Integer academicYearId,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer shiftId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Integer studentGroupId,
            @RequestParam(required = false, defaultValue = "true") Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "classRoll") String sort,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC")
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        return ResponseEntity.ok(enrollmentService.getEnrollments(
                academicYearId, classId, sectionId,
                shiftId, genderSectionId, studentGroupId,
                isActive, pageable
        ));
    }

    /**
     * GET /api/enrollments/inactive
     * Fetch inactive enrollments for the current active academic year
     */
    @GetMapping("/inactive")
    public ResponseEntity<Page<EnrollmentResponseDto>> getInactiveEnrollments(
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer shiftId,
            @RequestParam(required = false) Integer genderSectionId,
            @RequestParam(required = false) Integer studentGroupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "classRoll") String sort,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC")
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        return ResponseEntity.ok(enrollmentService.getInactiveEnrollmentsForCurrentYear(
                classId, sectionId, shiftId, genderSectionId, studentGroupId, pageable
        ));
    }

    /**
     * GET /api/enrollments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentResponseDto> getEnrollmentById(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentById(id));
    }

    /**
     * GET /api/enrollments/student/{studentSystemId}
     * Get all enrollments for a student across all years
     */
    @GetMapping("/student/{studentSystemId}")
    public ResponseEntity<List<EnrollmentResponseDto>> getEnrollmentsByStudentSystemId(
            @PathVariable String studentSystemId
    ) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByStudentSystemId(studentSystemId));
    }

    /**
     * POST /api/enrollments
     * Body: { studentSystemId, academicYear: {id}, studentClass: {id},
     *         section: {id}, shift: {id}, genderSection: {id},
     *         studentGroup: {id}, classRoll }
     */
    @PostMapping
    public ResponseEntity<EnrollmentResponseDto> createEnrollment(
            @RequestBody Enrollment enrollment
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentService.createEnrollment(enrollment));
    }

    /**
     * PUT /api/enrollments/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<EnrollmentResponseDto> updateEnrollment(
            @PathVariable Long id,
            @RequestBody Enrollment enrollment
    ) {
        return ResponseEntity.ok(enrollmentService.updateEnrollment(id, enrollment));
    }

    /**
     * PATCH /api/enrollments/{id}/activate — reactivate a soft deleted enrollment
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<EnrollmentResponseDto> activateEnrollment(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentService.activateEnrollment(id));
    }

    /**
     * DELETE /api/enrollments/{id} — soft delete
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEnrollment(@PathVariable Long id) {
        enrollmentService.deleteEnrollment(id);
        return ResponseEntity.ok("Enrollment deactivated successfully");
    }
}