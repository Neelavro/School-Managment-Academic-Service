package com.example.academic_service.service.impl;

import com.example.academic_service.client.StudentServiceClient;
import com.example.academic_service.dto.EnrollmentResponseDto;
import com.example.academic_service.dto.EnrollmentWithStudentRequestDto;
import com.example.academic_service.dto.StudentDto;
import com.example.academic_service.entity.*;
import com.example.academic_service.entity.Class;
import com.example.academic_service.repository.*;
import com.example.academic_service.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final ShiftRepository shiftRepository;
    private final GenderSectionRepository genderSectionRepository;
    private final StudentGroupRepository studentGroupRepository;

    // ── Get paginated + filtered enrollments ──────────────────────────────────
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
            String search,
            Pageable pageable
    ) {
        Specification<Enrollment> spec = EnrollmentSpecification.filter(
                academicYearId, classId, sectionId,
                shiftId, genderSectionId, studentGroupId, isActive, search
        );

        Page<Enrollment> enrollmentPage = enrollmentRepository.findAll(spec, pageable);

        if (enrollmentPage.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<String, StudentDto> studentMap = studentServiceClient.fetchAllStudentsAsMap();

        // If search term exists, also filter by student name (lives in student-service)
        String searchLower = (search != null && !search.isBlank()) ? search.toLowerCase() : null;

        List<EnrollmentResponseDto> result = enrollmentPage.getContent().stream()
                .map(enrollment -> {
                    StudentDto student = studentMap.get(enrollment.getStudentSystemId());
                    if (student == null) {
                        log.warn("No student found in student-service for systemId: {}",
                                enrollment.getStudentSystemId());
                        return null;
                    }
                    // Post-filter by name if search term provided
                    if (searchLower != null) {
                        boolean nameMatch =
                                (student.getNameEnglish() != null &&
                                        student.getNameEnglish().toLowerCase().contains(searchLower)) ||
                                        (student.getNameBangla() != null &&
                                                student.getNameBangla().toLowerCase().contains(searchLower));
                        boolean idMatch = enrollment.getStudentSystemId() != null &&
                                enrollment.getStudentSystemId().toLowerCase().contains(searchLower);
                        boolean rollMatch = enrollment.getClassRoll() != null &&
                                enrollment.getClassRoll().toString().contains(searchLower);
                        if (!nameMatch && !idMatch && !rollMatch) return null;
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
                shiftId, genderSectionId, studentGroupId, false, null
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
    public EnrollmentResponseDto createEnrollment(
            EnrollmentWithStudentRequestDto request, MultipartFile image) {

        // 1. Check if student already exists
        Map<String, StudentDto> studentMap = studentServiceClient.fetchAllStudentsAsMap();
        StudentDto student = null;

        if (request.getStudentSystemId() != null) {
            student = studentMap.get(request.getStudentSystemId());
        }

        Long studentDbId = null;

        if (student == null) {
            // 2. Create student in student-service → get DB id
            log.info("Student not found for systemId: {} — creating in student-service",
                    request.getStudentSystemId());
            studentDbId = studentServiceClient.createStudent(request);

            // 3. Upload image using DB id
            studentServiceClient.uploadStudentImage(studentDbId, image);

            // 4. Re-fetch to get full StudentDto (needed for response)
            studentMap = studentServiceClient.fetchAllStudentsAsMap();
            student    = studentMap.get(request.getStudentSystemId());

            if (student == null) {
                throw new RuntimeException(
                        "Student was created but could not be fetched back: "
                                + request.getStudentSystemId());
            }
        }

        // 5. Duplicate enrollment check
        if (request.getAcademicYearId() != null &&
                enrollmentRepository.existsByStudentSystemIdAndAcademicYearId(
                        student.getStudentSystemId(), request.getAcademicYearId())) {
            throw new RuntimeException(
                    "Student is already enrolled in this academic year: "
                            + student.getStudentSystemId());
        }

        // 6. Build and save enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentSystemId(student.getStudentSystemId());
        enrollment.setClassRoll(request.getClassRoll());
        enrollment.setIsActive(true);

        if (request.getAcademicYearId() != null)
            enrollment.setAcademicYear(academicYearRepository.findById(request.getAcademicYearId())
                    .orElseThrow(() -> new RuntimeException("AcademicYear not found: " + request.getAcademicYearId())));
        if (request.getClassId() != null)
            enrollment.setStudentClass(classRepository.findById(request.getClassId())
                    .orElseThrow(() -> new RuntimeException("Class not found: " + request.getClassId())));
        if (request.getSectionId() != null)
            enrollment.setSection(sectionRepository.findById(Math.toIntExact(request.getSectionId()))
                    .orElseThrow(() -> new RuntimeException("Section not found: " + request.getSectionId())));
        if (request.getShiftId() != null)
            enrollment.setShift(shiftRepository.findById(request.getShiftId())
                    .orElseThrow(() -> new RuntimeException("Shift not found: " + request.getShiftId())));
        if (request.getGenderSectionId() != null)
            enrollment.setGenderSection(genderSectionRepository.findById(request.getGenderSectionId())
                    .orElseThrow(() -> new RuntimeException("GenderSection not found: " + request.getGenderSectionId())));
        if (request.getStudentGroupId() != null)
            enrollment.setStudentGroup(studentGroupRepository.findById(request.getStudentGroupId())
                    .orElseThrow(() -> new RuntimeException("StudentGroup not found: " + request.getStudentGroupId())));

        Enrollment saved = enrollmentRepository.save(enrollment);
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