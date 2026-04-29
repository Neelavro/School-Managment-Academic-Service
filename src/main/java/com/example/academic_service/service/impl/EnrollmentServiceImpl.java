package com.example.academic_service.service.impl;

import com.example.academic_service.dto.EnrollmentResponseDto;
import com.example.academic_service.dto.EnrollmentWithStudentRequestDto;
import com.example.academic_service.entity.*;
import com.example.academic_service.entity.Class;
import com.example.academic_service.repository.*;
import com.example.academic_service.service.EnrollmentService;
import com.example.academic_service.service.StudentImageService;
import com.example.academic_service.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final StudentImageService studentImageService;
    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final ShiftRepository shiftRepository;
    private final GenderSectionRepository genderSectionRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final StudentStatusRepository studentStatusRepository;

    @Override
    public List<StudentStatus> getStudentStatus() {
        return studentStatusRepository.findAll();
    }

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
            Integer startRoll,
            Integer endRoll,
            Pageable pageable
    ) {
        Specification<Enrollment> spec = EnrollmentSpecification.filter(
                academicYearId, classId, sectionId,
                shiftId, genderSectionId, studentGroupId, isActive, search,
                startRoll, endRoll
        );

        Page<Enrollment> enrollmentPage = enrollmentRepository.findAll(spec, pageable);

        if (enrollmentPage.isEmpty()) {
            return Page.empty(pageable);
        }

        String searchLower = (search != null && !search.isBlank()) ? search.toLowerCase() : null;

        List<EnrollmentResponseDto> result = enrollmentPage.getContent().stream()
                .map(enrollment -> {
                    Student student = enrollment.getStudent();
                    if (student == null) {
                        log.warn("No student found for systemId: {}", enrollment.getStudentSystemId());
                        return null;
                    }
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
                shiftId, genderSectionId, studentGroupId, false, null,
                null, null
        );

        Page<Enrollment> enrollmentPage = enrollmentRepository.findAll(spec, pageable);

        if (enrollmentPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<EnrollmentResponseDto> result = enrollmentPage.getContent().stream()
                .map(enrollment -> {
                    Student student = enrollment.getStudent();
                    if (student == null) {
                        log.warn("No student found for systemId: {}", enrollment.getStudentSystemId());
                        return null;
                    }
                    return EnrollmentResponseDto.from(enrollment, student);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PageImpl<>(result, pageable, enrollmentPage.getTotalElements());
    }

    // ── Update class roll ─────────────────────────────────────────────────────
    @Override
    public EnrollmentResponseDto updateClassRoll(Long id, Integer classRoll) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + id));
        enrollment.setClassRoll(classRoll);
        Enrollment saved = enrollmentRepository.save(enrollment);

        Student student = saved.getStudent();
        if (student == null)
            throw new RuntimeException("Student not found: " + saved.getStudentSystemId());
        return EnrollmentResponseDto.from(saved, student);
    }

    // ── Get single enrollment by id ───────────────────────────────────────────
    @Override
    public EnrollmentResponseDto getEnrollmentById(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + id));

        Student student = enrollment.getStudent();
        if (student == null)
            throw new RuntimeException("Student not found for systemId: " + enrollment.getStudentSystemId());
        return EnrollmentResponseDto.from(enrollment, student);
    }

    // ── Get all enrollments for a student ─────────────────────────────────────
    @Override
    public List<EnrollmentResponseDto> getEnrollmentsByStudentSystemId(String studentSystemId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentSystemId(studentSystemId);

        if (enrollments.isEmpty()) return List.of();

        Student student = studentRepository.findByStudentSystemId(studentSystemId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentSystemId));

        return enrollments.stream()
                .map(enrollment -> EnrollmentResponseDto.from(enrollment, student))
                .collect(Collectors.toList());
    }

    // ── Create enrollment ─────────────────────────────────────────────────────
    @Override
    public EnrollmentResponseDto createEnrollment(
            EnrollmentWithStudentRequestDto request, MultipartFile image) {

        // 1. Check if student already exists
        Student student = null;
        if (request.getStudentSystemId() != null && !request.getStudentSystemId().isBlank()) {
            student = studentRepository.findByStudentSystemId(request.getStudentSystemId()).orElse(null);
        }

        if (student == null) {
            // 2. Build and create student locally
            log.info("Student not found for systemId: {} — creating", request.getStudentSystemId());
            student = studentService.createStudent(buildStudentFromRequest(request));

            // 3. Upload image
            if (image != null && !image.isEmpty()) {
                studentImageService.addImage(student.getId(), image);
                // Reload to pick up the image reference
                student = studentRepository.findById(student.getId()).orElse(student);
            }
        }

        // 4. Duplicate enrollment check
        if (request.getAcademicYearId() != null &&
                enrollmentRepository.existsByStudentSystemIdAndAcademicYearId(
                        student.getStudentSystemId(), request.getAcademicYearId())) {
            throw new RuntimeException(
                    "Student is already enrolled in this academic year: " + student.getStudentSystemId());
        }

        // 5. Build and save enrollment
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
    public EnrollmentResponseDto updateEnrollment(Long id, EnrollmentWithStudentRequestDto request) {
        Enrollment existing = enrollmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + id));

        if (request.getAcademicYearId() != null)
            existing.setAcademicYear(academicYearRepository.findById(request.getAcademicYearId()).orElse(null));
        if (request.getClassId() != null)
            existing.setStudentClass(classRepository.findById(request.getClassId()).orElse(null));
        if (request.getSectionId() != null)
            existing.setSection(sectionRepository.findById(Math.toIntExact(request.getSectionId())).orElse(null));
        if (request.getShiftId() != null)
            existing.setShift(shiftRepository.findById(request.getShiftId()).orElse(null));
        if (request.getGenderSectionId() != null)
            existing.setGenderSection(genderSectionRepository.findById(request.getGenderSectionId()).orElse(null));
        if (request.getStudentGroupId() != null)
            existing.setStudentGroup(studentGroupRepository.findById(request.getStudentGroupId()).orElse(null));
        if (request.getClassRoll() != null)
            existing.setClassRoll(request.getClassRoll());
        if (request.getIsActive() != null)
            existing.setIsActive(request.getIsActive());
        else if (existing.getIsActive() == null)
            existing.setIsActive(true);

        Enrollment saved = enrollmentRepository.save(existing);

        // Update student personal info locally
        Student updatedStudent = studentService.updateStudentBySystemId(
                saved.getStudentSystemId(), buildStudentFromRequest(request));
        if (updatedStudent == null)
            throw new RuntimeException("Student not found: " + saved.getStudentSystemId());

        return EnrollmentResponseDto.from(saved, updatedStudent);
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

        Student student = saved.getStudent();
        if (student == null)
            throw new RuntimeException("Student not found: " + saved.getStudentSystemId());
        return EnrollmentResponseDto.from(saved, student);
    }

    // ── Helper: build Student entity from request ─────────────────────────────

    private Student buildStudentFromRequest(EnrollmentWithStudentRequestDto request) {
        Student student = new Student();
        student.setStudentSystemId(request.getStudentSystemId());
        student.setNameEnglish(request.getNameEnglish());
        student.setNameBangla(request.getNameBangla());
        student.setFatherNameEnglish(request.getFatherNameEnglish());
        student.setFatherNameBangla(request.getFatherNameBangla());
        student.setFatherOccupation(request.getFatherOccupation());
        student.setFatherPhone(request.getFatherPhone());
        student.setFatherMonthlySalary(request.getFatherMonthlySalary());
        student.setMotherNameEnglish(request.getMotherNameEnglish());
        student.setMotherNameBangla(request.getMotherNameBangla());
        student.setMotherOccupation(request.getMotherOccupation());
        student.setMotherPhone(request.getMotherPhone());
        student.setMotherMonthlySalary(request.getMotherMonthlySalary());
        student.setGuardianNameEnglish(request.getGuardianNameEnglish());
        student.setGuardianNameBangla(request.getGuardianNameBangla());
        student.setGuardianOccupation(request.getGuardianOccupation());
        student.setGuardianPhone(request.getGuardianPhone());
        student.setGuardianRelation(request.getGuardianRelation());
        student.setCurrentHoldingNo(request.getCurrentHoldingNo());
        student.setCurrentRoadOrVillage(request.getCurrentRoadOrVillage());
        student.setCurrentDistrict(request.getCurrentDistrict());
        student.setCurrentThana(request.getCurrentThana());
        student.setPermanentHoldingNo(request.getPermanentHoldingNo());
        student.setPermanentRoadOrVillage(request.getPermanentRoadOrVillage());
        student.setPermanentDistrict(request.getPermanentDistrict());
        student.setPermanentThana(request.getPermanentThana());
        student.setNationality(request.getNationality());
        student.setIsActive(request.getIsActive());

        if (request.getDob() != null && !request.getDob().isBlank()) {
            student.setDob(LocalDate.parse(request.getDob()));
        }
        if (request.getGenderId() != null) {
            Gender g = new Gender();
            g.setId(request.getGenderId());
            student.setGender(g);
        }
        if (request.getStudentStatusId() != null) {
            StudentStatus s = new StudentStatus();
            s.setId(request.getStudentStatusId());
            student.setStudentStatus(s);
        }

        return student;
    }
}
