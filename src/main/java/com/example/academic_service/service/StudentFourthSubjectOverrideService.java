package com.example.academic_service.service;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.StudentFourthSubjectOverrideDto;
import com.example.academic_service.entity.StudentFourthSubjectOverride;
import com.example.academic_service.repository.EnrollmentRepository;
import com.example.academic_service.repository.StudentFourthSubjectOverrideRepository;
import com.example.academic_service.repository.SubjectRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentFourthSubjectOverrideService {

    private final StudentFourthSubjectOverrideRepository repository;
    private final SubjectRepository subjectRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public ApiResponse<StudentFourthSubjectOverrideDto.Response> setOverride(StudentFourthSubjectOverrideDto.Request request) {
        enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + request.getEnrollmentId()));
        var subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found: " + request.getSubjectId()));

        StudentFourthSubjectOverride override = repository.findByEnrollmentId(request.getEnrollmentId())
                .orElse(new StudentFourthSubjectOverride());
        override.setEnrollmentId(request.getEnrollmentId());
        override.setSubject(subject);
        repository.save(override);
        return ApiResponse.success("Override saved", StudentFourthSubjectOverrideDto.Response.from(override));
    }

    public ApiResponse<StudentFourthSubjectOverrideDto.Response> getByEnrollment(Long enrollmentId) {
        return repository.findByEnrollmentId(enrollmentId)
                .map(o -> ApiResponse.success("Found", StudentFourthSubjectOverrideDto.Response.from(o)))
                .orElse(ApiResponse.success("No override set", null));
    }

    public ApiResponse<List<StudentFourthSubjectOverrideDto.Response>> getByClass(Integer classId) {
        List<Long> enrollmentIds = enrollmentRepository
                .findAllByClassIdAndFilters(classId, null, null, null, null, null, null)
                .stream().map(e -> e.getId()).collect(Collectors.toList());
        List<StudentFourthSubjectOverrideDto.Response> result = repository.findByEnrollmentIdIn(enrollmentIds)
                .stream().map(StudentFourthSubjectOverrideDto.Response::from).collect(Collectors.toList());
        return ApiResponse.success("Found " + result.size() + " override(s)", result);
    }

    @Transactional
    public ApiResponse<Void> deleteOverride(Long enrollmentId) {
        repository.deleteByEnrollmentId(enrollmentId);
        return ApiResponse.success("Override removed", null);
    }
}
