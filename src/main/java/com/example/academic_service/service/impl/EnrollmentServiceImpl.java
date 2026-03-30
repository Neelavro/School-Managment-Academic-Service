package com.example.academic_service.service.impl;

import com.example.academic_service.client.StudentServiceClient;
import com.example.academic_service.dto.EnrollmentResponseDto;
import com.example.academic_service.dto.StudentDto;
import com.example.academic_service.entity.AcademicYear;
import com.example.academic_service.entity.Enrollment;
import com.example.academic_service.repository.AcademicYearRepository;
import com.example.academic_service.repository.EnrollmentRepository;
import com.example.academic_service.repository.EnrollmentSpecification;
import com.example.academic_service.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentServiceClient studentServiceClient;
    private final AcademicYearRepository academicYearRepository;

    // ── Get paginated + filtered enrollments ──────────────────────────────────
    @Override
    public Page<EnrollmentResponseDto> getEnrollments(
            Integer academicYearId,
            Integer classId,
            Long sectionId,
            Integer shiftId,
            Integer genderSectionId,
            Integer studentGroupId,
            Boolean isActive,
            Pageable pageable
    ) {
        Specification<Enrollment> spec = EnrollmentSpecification.filter(
                academicYearId, classId, sectionId,
                shiftId, genderSectionId, studentGroupId, isActive
        );

        Page<Enrollment> enrollmentPage = enrollmentRepository.findAll(spec, pageable);

        if (enrollmentPage.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<String, StudentDto> studentMap = studentServiceClient.fetchAllStudentsAsMap();

        List<EnrollmentResponseDto> result = enrollmentPage.getContent().stream()
                .map(enrollment -> {
                    StudentDto student = studentMap.get(enrollment.getStudentSystemId());
                    if (student == null) {
                        log.warn("No student found in student-service for systemId: {}",
                                enrollment.getStudentSystemId());
                        return null;
                    }
                    return EnrollmentResponseDto.from(enrollment, student);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PageImpl<>(result, pageable, enrollmentPage.getTotalElements());
    }

    // ── Get inactive enrollments for current active academic year ─────────────
    @Override
    public Page<EnrollmentResponseDto> getInactiveEnrollmentsForCurrentYear(
            Integer classId,
            Long sectionId,
            Integer shiftId,
            Integer genderSectionId,
            Integer studentGroupId,
            Pageable pageable
    ) {
        AcademicYear activeYear = (AcademicYear) academicYearRepository.findByIsActiveTrue();

        Specification<Enrollment> spec = EnrollmentSpecification.filter(
                activeYear.getId(), classId, sectionId,
                shiftId, genderSectionId, studentGroupId, false  // isActive = false
        );

        Page<Enrollment> enrollmentPage = enrollmentRepository.findAll(spec, pageable);

        if (enrollmentPage.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<String, StudentDto> studentMap = studentServiceClient.fetchAllStudentsAsMap();

        List<EnrollmentResponseDto> result = enrollmentPage.getContent().stream()
                .map(enrollment -> {
                    StudentDto student = studentMap.get(enrollment.getStudentSystemId());
                    if (student == null) {
                        log.warn("No student found in student-service for systemId: {}",
                                enrollment.getStudentSystemId());
                        return null;
                    }
                    return EnrollmentResponseDto.from(enrollment, student);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PageImpl<>(result, pageable, enrollmentPage.getTotalElements());
    }

    // ── Get single enrollment by id ───────────────────────────────────────────
    @Override
    public EnrollmentResponseDto getEnrollmentById(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + id));

        Map<String, StudentDto> studentMap = studentServiceClient.fetchAllStudentsAsMap();
        StudentDto student = studentMap.get(enrollment.getStudentSystemId());
        if (student == null) {
            throw new RuntimeException("Student not found in student-service: "
                    + enrollment.getStudentSystemId());
        }
        return EnrollmentResponseDto.from(enrollment, student);
    }

    // ── Get all enrollments for a student ─────────────────────────────────────
    @Override
    public List<EnrollmentResponseDto> getEnrollmentsByStudentSystemId(String studentSystemId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentSystemId(studentSystemId);

        if (enrollments.isEmpty()) {
            return List.of();
        }

        Map<String, StudentDto> studentMap = studentServiceClient.fetchAllStudentsAsMap();
        StudentDto student = studentMap.get(studentSystemId);
        if (student == null) {
            throw new RuntimeException("Student not found in student-service: " + studentSystemId);
        }

        return enrollments.stream()
                .map(enrollment -> EnrollmentResponseDto.from(enrollment, student))
                .collect(Collectors.toList());
    }

    // ── Create enrollment ─────────────────────────────────────────────────────
    @Override
    public EnrollmentResponseDto createEnrollment(Enrollment enrollment) {
        // Prevent duplicate enrollment in same academic year
        if (enrollment.getAcademicYear() != null &&
                enrollmentRepository.existsByStudentSystemIdAndAcademicYearId(
                        enrollment.getStudentSystemId(),
                        enrollment.getAcademicYear().getId())) {
            throw new RuntimeException("Student is already enrolled in this academic year: "
                    + enrollment.getStudentSystemId());
        }

        Enrollment saved = enrollmentRepository.save(enrollment);

        Map<String, StudentDto> studentMap = studentServiceClient.fetchAllStudentsAsMap();
        StudentDto student = studentMap.get(saved.getStudentSystemId());
        if (student == null) {
            throw new RuntimeException("Student not found in student-service: "
                    + saved.getStudentSystemId());
        }
        return EnrollmentResponseDto.from(saved, student);
    }

    // ── Update enrollment ─────────────────────────────────────────────────────
    @Override
    public EnrollmentResponseDto updateEnrollment(Long id, Enrollment updated) {
        Enrollment existing = enrollmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + id));

        // Only update fields that are provided (null-safe)
        if (updated.getAcademicYear() != null) existing.setAcademicYear(updated.getAcademicYear());
        if (updated.getStudentClass() != null) existing.setStudentClass(updated.getStudentClass());
        if (updated.getSection() != null) existing.setSection(updated.getSection());
        if (updated.getShift() != null) existing.setShift(updated.getShift());
        if (updated.getGenderSection() != null) existing.setGenderSection(updated.getGenderSection());
        if (updated.getStudentGroup() != null) existing.setStudentGroup(updated.getStudentGroup());
        if (updated.getClassRoll() != null) existing.setClassRoll(updated.getClassRoll());
        if (updated.getIsActive() != null) existing.setIsActive(updated.getIsActive());

        Enrollment saved = enrollmentRepository.save(existing);

        Map<String, StudentDto> studentMap = studentServiceClient.fetchAllStudentsAsMap();
        StudentDto student = studentMap.get(saved.getStudentSystemId());
        if (student == null) {
            throw new RuntimeException("Student not found in student-service: "
                    + saved.getStudentSystemId());
        }
        return EnrollmentResponseDto.from(saved, student);
    }

    // ── Soft delete ───────────────────────────────────────────────────────────
    @Override
    public void deleteEnrollment(Long id) {
        Enrollment existing = enrollmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + id));
        existing.setIsActive(false);
        enrollmentRepository.save(existing);
    }

    // ── Reactivate ────────────────────────────────────────────────────────────
    @Override
    public EnrollmentResponseDto activateEnrollment(Long id) {
        Enrollment existing = enrollmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + id));
        existing.setIsActive(true);
        Enrollment saved = enrollmentRepository.save(existing);

        Map<String, StudentDto> studentMap = studentServiceClient.fetchAllStudentsAsMap();
        StudentDto student = studentMap.get(saved.getStudentSystemId());
        if (student == null) {
            throw new RuntimeException("Student not found in student-service: "
                    + saved.getStudentSystemId());
        }
        return EnrollmentResponseDto.from(saved, student);
    }
}