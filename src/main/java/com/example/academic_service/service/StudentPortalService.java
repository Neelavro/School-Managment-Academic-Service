package com.example.academic_service.service;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.result_dtos.StudentRoutineResultResponse;
import com.example.academic_service.dto.student_portal_dtos.StudentPortalProfileDto;
import com.example.academic_service.dto.student_portal_dtos.UpcomingExamDto;
import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import com.example.academic_service.repository.ResultPublicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentPortalService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ClassRoutineRepository classRoutineRepository;
    private final ExamRoutineRepository examRoutineRepository;
    private final ResultPublicationRepository resultPublicationRepository;
    private final ResultService resultService;
    private final PasswordEncoder passwordEncoder;

    private Enrollment requireActiveEnrollment(String studentSystemId) {
        return enrollmentRepository.findActiveEnrollmentByStudentSystemId(studentSystemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active enrollment found for this student"));
    }

    public ApiResponse<StudentPortalProfileDto> getProfile(String studentSystemId) {
        Student student = studentRepository.findByStudentSystemId(studentSystemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        Enrollment enrollment = enrollmentRepository.findActiveEnrollmentByStudentSystemId(studentSystemId)
                .orElse(null);
        return ApiResponse.success("Profile fetched", StudentPortalProfileDto.from(student, enrollment));
    }

    public ApiResponse<Map<String, Object>> getWeeklySchedule(String studentSystemId) {
        Enrollment e = requireActiveEnrollment(studentSystemId);

        Integer classId = e.getStudentClass() != null ? e.getStudentClass().getId() : null;
        if (classId == null) return ApiResponse.success("No class assigned", Map.of("schedule", Map.of()));

        Integer genderSectionId = e.getGenderSection() != null ? e.getGenderSection().getId() : null;
        Long sectionId = e.getSection() != null ? e.getSection().getId() : null;
        Integer groupId = e.getStudentGroup() != null ? e.getStudentGroup().getId() : null;

        List<ClassRoutine> routines = classRoutineRepository
                .findStudentWeeklySchedule(classId, genderSectionId, sectionId, groupId);

        List<String> dayOrder = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
        Map<String, List<Map<String, Object>>> byDay = new LinkedHashMap<>();
        for (String day : dayOrder) {
            List<Map<String, Object>> periods = routines.stream()
                    .filter(r -> r.getDayOfWeek().name().equals(day))
                    .map(r -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", r.getId());
                        m.put("subjectName", r.getSubject() != null ? r.getSubject().getName() : null);
                        m.put("subjectCode", r.getSubject() != null ? r.getSubject().getCode() : null);
                        m.put("roomName", r.getRoom() != null ? r.getRoom().getName() : null);
                        m.put("startTime", r.getStartTime() != null ? r.getStartTime().toString() : null);
                        m.put("endTime", r.getEndTime() != null ? r.getEndTime().toString() : null);
                        return m;
                    })
                    .collect(Collectors.toList());
            if (!periods.isEmpty()) byDay.put(day, periods);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("className", e.getStudentClass().getName());
        response.put("schedule", byDay);
        return ApiResponse.success("Weekly schedule fetched", response);
    }

    public ApiResponse<List<UpcomingExamDto>> getUpcomingExams(String studentSystemId) {
        Enrollment e = requireActiveEnrollment(studentSystemId);

        Integer classId = e.getStudentClass() != null ? e.getStudentClass().getId() : null;
        if (classId == null) return ApiResponse.success("No class assigned", List.of());

        Integer groupId = e.getStudentGroup() != null ? e.getStudentGroup().getId() : null;

        List<UpcomingExamDto> result = examSessionRepository
                .findUpcomingForStudent(classId, groupId, LocalDate.now())
                .stream().map(UpcomingExamDto::from).collect(Collectors.toList());

        return ApiResponse.success("Upcoming exams fetched", result);
    }

    public ApiResponse<List<Map<String, Object>>> getAvailableRoutines(String studentSystemId) {
        Enrollment e = requireActiveEnrollment(studentSystemId);
        Integer academicYearId = e.getAcademicYear() != null ? e.getAcademicYear().getId() : null;
        Integer classId = e.getStudentClass() != null ? e.getStudentClass().getId() : null;
        if (academicYearId == null || classId == null) return ApiResponse.success("No academic year or class", List.of());

        List<ExamRoutine> allRoutines = examRoutineRepository.findByAcademicYearIdAndIsActiveTrue(academicYearId);

        List<Integer> routineIds = allRoutines.stream().map(ExamRoutine::getId).collect(Collectors.toList());
        Set<Integer> resultPublishedIds = routineIds.isEmpty()
                ? Set.of()
                : resultPublicationRepository.findPublishedRoutineIdsForClass(routineIds, classId);

        List<Map<String, Object>> routines = allRoutines.stream()
                .filter(r -> resultPublishedIds.contains(r.getId()))
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("title", r.getTitle());
                    m.put("examTypeName", r.getExamType() != null ? r.getExamType().getName() : null);
                    m.put("routineStartDate", r.getRoutineStartDate() != null ? r.getRoutineStartDate().toString() : null);
                    m.put("routineEndDate", r.getRoutineEndDate() != null ? r.getRoutineEndDate().toString() : null);
                    return m;
                })
                .collect(Collectors.toList());

        return ApiResponse.success("Routines fetched", routines);
    }

    public ApiResponse<StudentRoutineResultResponse> getMyResult(String studentSystemId, Integer examRoutineId) {
        Enrollment e = requireActiveEnrollment(studentSystemId);
        Integer classId = e.getStudentClass() != null ? e.getStudentClass().getId() : null;

        resultPublicationRepository.findByExamRoutine_IdAndStudentClass_Id(examRoutineId, classId)
                .filter(rp -> Boolean.TRUE.equals(rp.getPublished()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Results for this routine have not been released yet"));

        StudentRoutineResultResponse result = resultService.getStudentRoutineResult(e.getId(), examRoutineId);
        return ApiResponse.success("Result fetched", result);
    }

    public ApiResponse<Void> changePassword(String studentSystemId, String currentPassword, String newPassword) {
        Student student = studentRepository.findByStudentSystemId(studentSystemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        String hash = student.getPasswordHash();
        if (hash == null || !passwordEncoder.matches(currentPassword, hash))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");

        student.setPasswordHash(passwordEncoder.encode(newPassword));
        studentRepository.save(student);
        return ApiResponse.success("Password changed successfully", null);
    }
}
