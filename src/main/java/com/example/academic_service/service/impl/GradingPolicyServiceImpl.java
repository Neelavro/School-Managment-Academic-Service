package com.example.academic_service.service.impl;

import com.example.academic_service.dto.grading_dtos.GradeResponseDto;
import com.example.academic_service.dto.grading_dtos.GradingPolicyRequestDto;
import com.example.academic_service.dto.grading_dtos.GradingPolicyResponseDto;
import com.example.academic_service.entity.Grade;
import com.example.academic_service.entity.GradingPolicy;
import com.example.academic_service.repository.GradeRepository;
import com.example.academic_service.repository.GradingPolicyRepository;
import com.example.academic_service.service.GradingPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradingPolicyServiceImpl implements GradingPolicyService {

    private final GradingPolicyRepository gradingPolicyRepository;
    private final GradeRepository gradeRepository;

    @Override
    public GradingPolicyResponseDto create(GradingPolicyRequestDto dto) {
        if (dto.getName() == null || dto.getName().isBlank())
            throw new RuntimeException("Grading policy name is required");
        if (gradingPolicyRepository.existsByName(dto.getName()))
            throw new RuntimeException("Grading policy '" + dto.getName() + "' already exists");

        GradingPolicy policy = GradingPolicy.builder()
                .name(dto.getName())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
        return mapToDto(gradingPolicyRepository.save(policy));
    }

    @Override
    public GradingPolicyResponseDto getById(Long id) {
        return mapToDto(findOrThrow(id));
    }

    @Override
    public List<GradingPolicyResponseDto> getAll() {
        return gradingPolicyRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<GradingPolicyResponseDto> getByActive(Boolean isActive) {
        return gradingPolicyRepository.findByIsActive(isActive).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public GradingPolicyResponseDto update(Long id, GradingPolicyRequestDto dto) {
        GradingPolicy policy = findOrThrow(id);
        if (dto.getName() != null) {
            if (dto.getName().isBlank())
                throw new RuntimeException("Grading policy name cannot be blank");
            if (!policy.getName().equals(dto.getName()) && gradingPolicyRepository.existsByName(dto.getName()))
                throw new RuntimeException("Grading policy '" + dto.getName() + "' already exists");
            policy.setName(dto.getName());
        }
        if (dto.getIsActive() != null) policy.setIsActive(dto.getIsActive());
        return mapToDto(gradingPolicyRepository.save(policy));
    }

    @Override
    public void delete(Long id) {
        GradingPolicy policy = findOrThrow(id);
        if (!gradeRepository.findByGradingPolicyId(policy.getId()).isEmpty())
            throw new RuntimeException("Cannot delete grading policy '" + policy.getName() + "' because it has grades assigned to it");
        gradingPolicyRepository.delete(policy);
    }

    private GradingPolicy findOrThrow(Long id) {
        return gradingPolicyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grading policy not found with id: " + id));
    }

    private GradingPolicyResponseDto mapToDto(GradingPolicy policy) {
        List<GradeResponseDto> grades = gradeRepository.findByGradingPolicyId(policy.getId())
                .stream().map(this::mapGradeToDto).collect(Collectors.toList());
        return GradingPolicyResponseDto.builder()
                .id(policy.getId())
                .name(policy.getName())
                .isActive(policy.getIsActive())
                .grades(grades)
                .build();
    }

    private GradeResponseDto mapGradeToDto(Grade grade) {
        return GradeResponseDto.builder()
                .id(grade.getId())
                .gradingPolicyId(grade.getGradingPolicy().getId())
                .name(grade.getName())
                .gpaValue(grade.getGpaValue())
                .minMark(grade.getMinMark())
                .maxMark(grade.getMaxMark())
                .build();
    }
}