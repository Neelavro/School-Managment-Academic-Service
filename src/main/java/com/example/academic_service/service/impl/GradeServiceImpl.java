package com.example.academic_service.service.impl;

import com.example.academic_service.dto.grading_dtos.*;
import com.example.academic_service.entity.Grade;
import com.example.academic_service.entity.GradingPolicy;
import com.example.academic_service.repository.GradeRepository;
import com.example.academic_service.repository.GradingPolicyRepository;
import com.example.academic_service.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class GradeServiceImpl implements GradeService {

    private final GradeRepository gradeRepository;
    private final GradingPolicyRepository gradingPolicyRepository;

    @Override
    public GradeResponseDto create(GradeRequestDto dto) {
        if (dto.getGradingPolicyId() == null) throw new RuntimeException("Grading policy id is required");
        if (dto.getName() == null) throw new RuntimeException("Grade name is required");
        if (dto.getGpaValue() == null) throw new RuntimeException("GPA value is required");
        if (dto.getMinMark() == null) throw new RuntimeException("Min mark is required");
        if (dto.getMaxMark() == null) throw new RuntimeException("Max mark is required");
        if (dto.getMinMark() > dto.getMaxMark()) throw new RuntimeException("Min mark cannot be greater than max mark");

        GradingPolicy policy = gradingPolicyRepository.findById(dto.getGradingPolicyId())
                .orElseThrow(() -> new RuntimeException("Grading policy not found with id: " + dto.getGradingPolicyId()));

        if (gradeRepository.existsByNameAndGradingPolicyId(dto.getName(), dto.getGradingPolicyId()))
            throw new RuntimeException("Grade '" + dto.getName() + "' already exists in this policy");

        Grade grade = Grade.builder()
                .gradingPolicy(policy)
                .name(dto.getName())
                .gpaValue(dto.getGpaValue())
                .minMark(dto.getMinMark())
                .maxMark(dto.getMaxMark())
                .comment(dto.getComment())
                .build();
        return mapToDto(gradeRepository.save(grade));
    }

    @Override
    public GradeResponseDto getById(Long id) {
        return mapToDto(findOrThrow(id));
    }

    @Override
    public List<GradeResponseDto> getAll() {
        return gradeRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public GradeResponseDto update(Long id, GradeRequestDto dto) {
        Grade grade = findOrThrow(id);

        if (dto.getGradingPolicyId() != null) {
            GradingPolicy policy = gradingPolicyRepository.findById(dto.getGradingPolicyId())
                    .orElseThrow(() -> new RuntimeException("Grading policy not found with id: " + dto.getGradingPolicyId()));
            grade.setGradingPolicy(policy);
        }

        if (dto.getName() != null) {
            Long policyId = grade.getGradingPolicy().getId();
            if (!grade.getName().equals(dto.getName()) &&
                    gradeRepository.existsByNameAndGradingPolicyId(dto.getName(), policyId))
                throw new RuntimeException("Grade '" + dto.getName() + "' already exists in this policy");
            grade.setName(dto.getName());
        }

        if (dto.getMinMark() != null && dto.getMaxMark() != null && dto.getMinMark() > dto.getMaxMark())
            throw new RuntimeException("Min mark cannot be greater than max mark");
        if (dto.getMinMark() != null && dto.getMaxMark() == null && dto.getMinMark() > grade.getMaxMark())
            throw new RuntimeException("Min mark cannot be greater than existing max mark: " + grade.getMaxMark());
        if (dto.getMaxMark() != null && dto.getMinMark() == null && dto.getMaxMark() < grade.getMinMark())
            throw new RuntimeException("Max mark cannot be less than existing min mark: " + grade.getMinMark());

        if (dto.getGpaValue() != null) grade.setGpaValue(dto.getGpaValue());
        if (dto.getMinMark() != null) grade.setMinMark(dto.getMinMark());
        if (dto.getMaxMark() != null) grade.setMaxMark(dto.getMaxMark());
        if (dto.getComment() != null) grade.setComment(dto.getComment());

        return mapToDto(gradeRepository.save(grade));
    }

    @Override
    public void delete(Long id) {
        gradeRepository.delete(findOrThrow(id));
    }

    private Grade findOrThrow(Long id) {
        return gradeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grade not found with id: " + id));
    }

    private GradeResponseDto mapToDto(Grade grade) {
        return GradeResponseDto.builder()
                .id(grade.getId())
                .gradingPolicyId(grade.getGradingPolicy().getId())
                .name(grade.getName())
                .gpaValue(grade.getGpaValue())
                .minMark(grade.getMinMark())
                .maxMark(grade.getMaxMark())
                .comment(grade.getComment())
                .build();
    }
}