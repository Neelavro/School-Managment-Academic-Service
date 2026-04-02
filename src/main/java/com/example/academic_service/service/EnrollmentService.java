package com.example.academic_service.service;

import com.example.academic_service.dto.EnrollmentResponseDto;
import com.example.academic_service.dto.EnrollmentWithStudentRequestDto;
import com.example.academic_service.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EnrollmentService {

    // Get paginated + filtered list
    Page<EnrollmentResponseDto> getEnrollments(
            Integer academicYearId,
            Integer classId,
            Long sectionId,
            Integer shiftId,
            Integer genderSectionId,
            Integer studentGroupId,
            Boolean isActive,
            Pageable pageable
    );

    // Get inactive enrollments for the current active academic year
    Page<EnrollmentResponseDto> getInactiveEnrollmentsForCurrentYear(
            Integer classId,
            Long sectionId,
            Integer shiftId,
            Integer genderSectionId,
            Integer studentGroupId,
            Pageable pageable
    );

    // Get single enrollment by id
    EnrollmentResponseDto getEnrollmentById(Long id);

    // Get all enrollments for a student across all years
    List<EnrollmentResponseDto> getEnrollmentsByStudentSystemId(String studentSystemId);

    EnrollmentResponseDto createEnrollment(EnrollmentWithStudentRequestDto request, MultipartFile image);

    EnrollmentResponseDto updateEnrollment(Long id, Enrollment enrollment);

    // Soft delete
    void deleteEnrollment(Long id);

    // Reactivate
    EnrollmentResponseDto activateEnrollment(Long id);
}