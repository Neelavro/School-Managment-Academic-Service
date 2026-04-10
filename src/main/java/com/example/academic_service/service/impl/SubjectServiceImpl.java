package com.example.academic_service.service.impl;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.SubjectRequestDto;
import com.example.academic_service.entity.Subject;
import com.example.academic_service.entity.Class;
import com.example.academic_service.repository.ClassRepository;
import com.example.academic_service.repository.SubjectRepository;
import com.example.academic_service.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;

    @Override
    public ApiResponse<Subject> create(SubjectRequestDto dto) {
        if (subjectRepository.existsByNameIgnoreCase(dto.getName())) {
            return ApiResponse.error("Subject with this name already exists");
        }
        if (dto.getCode() != null && subjectRepository.existsByCodeIgnoreCase(dto.getCode())) {
            return ApiResponse.error("Subject with this code already exists");
        }
        Subject subject = new Subject();
        subject.setName(dto.getName());
        subject.setCode(dto.getCode());
        return ApiResponse.success("Subject created successfully", subjectRepository.save(subject));
    }

    @Override
    public ApiResponse<Subject> update(Integer id, SubjectRequestDto dto) {
        Subject subject = subjectRepository.findById(id).orElse(null);
        if (subject == null) return ApiResponse.error("Subject not found");
        if (!subject.getIsActive()) return ApiResponse.error("Cannot update an inactive subject");
        if (subjectRepository.existsByNameIgnoreCaseAndIdNot(dto.getName(), id)) {
            return ApiResponse.error("Another subject with this name already exists");
        }
        if (dto.getCode() != null &&
                subjectRepository.existsByCodeIgnoreCaseAndIdNot(dto.getCode(), id)) {
            return ApiResponse.error("Another subject with this code already exists");
        }
        subject.setName(dto.getName());
        subject.setCode(dto.getCode());
        return ApiResponse.success("Subject updated successfully", subjectRepository.save(subject));
    }

    @Override
    public ApiResponse<Subject> getById(Integer id) {
        Subject subject = subjectRepository.findById(id).orElse(null);
        if (subject == null) return ApiResponse.error("Subject not found");
        return ApiResponse.success("Subject fetched successfully", subject);
    }

    @Override
    public ApiResponse<List<Subject>> getAll(Boolean active) {
        List<Subject> result;
        if (active == null) {
            result = subjectRepository.findAll();
        } else if (active) {
            result = subjectRepository.findByIsActiveTrue();
        } else {
            result = subjectRepository.findByIsActiveFalse();
        }
        return ApiResponse.success("Subjects fetched successfully", result);
    }

    @Override
    public ApiResponse<Subject> reactivate(Integer id) {
        Subject subject = subjectRepository.findById(id).orElse(null);
        if (subject == null) return ApiResponse.error("Subject not found");
        if (subject.getIsActive()) return ApiResponse.error("Subject is already active");

        if (subjectRepository.existsByNameIgnoreCaseAndIsActiveTrueAndIdNot(
                subject.getName(), id)) {
            return ApiResponse.error("An active subject with the name '" +
                    subject.getName() + "' already exists. Rename before reactivating");
        }
        if (subject.getCode() != null &&
                subjectRepository.existsByCodeIgnoreCaseAndIsActiveTrueAndIdNot(
                        subject.getCode(), id)) {
            return ApiResponse.error("An active subject with the code '" +
                    subject.getCode() + "' already exists. Rename before reactivating");
        }
        subject.setIsActive(true);
        return ApiResponse.success("Subject reactivated successfully",
                subjectRepository.save(subject));
    }

    @Override
    public ApiResponse<Void> delete(Integer id) {
        Subject subject = subjectRepository.findById(id).orElse(null);
        if (subject == null) return ApiResponse.error("Subject not found");
        if (!subject.getIsActive()) return ApiResponse.error("Subject is already inactive");
        subject.setIsActive(false);
        subjectRepository.save(subject);
        return ApiResponse.success("Subject deleted successfully", null);
    }
}