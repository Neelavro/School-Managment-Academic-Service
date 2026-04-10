package com.example.academic_service.service.impl;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.ExamTypeRequestDto;
import com.example.academic_service.entity.ExamType;
import com.example.academic_service.repository.ExamTypeRepository;
import com.example.academic_service.service.ExamTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamTypeServiceImpl implements ExamTypeService {

    private final ExamTypeRepository examTypeRepository;

    @Override
    public ApiResponse<ExamType> create(ExamTypeRequestDto dto) {
        if (examTypeRepository.existsByNameIgnoreCase(dto.getName())) {
            return ApiResponse.error("Exam type with this name already exists");
        }
        ExamType examType = new ExamType();
        examType.setName(dto.getName());
        examType.setOrderIndex(dto.getOrderIndex());
        return ApiResponse.success("Exam type created successfully", examTypeRepository.save(examType));
    }

    @Override
    public ApiResponse<ExamType> update(Integer id, ExamTypeRequestDto dto) {
        ExamType examType = examTypeRepository.findById(id).orElse(null);
        if (examType == null) return ApiResponse.error("Exam type not found");

        if (!examType.getIsActive()) return ApiResponse.error("Cannot update an inactive exam type");

        if (examTypeRepository.existsByNameIgnoreCaseAndIdNot(dto.getName(), id)) {
            return ApiResponse.error("Another exam type with this name already exists");
        }
        examType.setName(dto.getName());
        examType.setOrderIndex(dto.getOrderIndex());
        return ApiResponse.success("Exam type updated successfully", examTypeRepository.save(examType));
    }

    @Override
    public ApiResponse<List<ExamType>> getAll(Boolean active) {
        List<ExamType> result;
        if (active == null) {
            result = examTypeRepository.findAll();
        } else if (active) {
            result = examTypeRepository.findByIsActiveTrueOrderByOrderIndexAsc();
        } else {
            result = examTypeRepository.findByIsActiveFalseOrderByOrderIndexAsc();
        }
        return ApiResponse.success("Exam types fetched successfully", result);
    }

    @Override
    public ApiResponse<ExamType> reactivate(Integer id) {
        ExamType examType = examTypeRepository.findById(id).orElse(null);
        if (examType == null) return ApiResponse.error("Exam type not found");

        if (examType.getIsActive()) return ApiResponse.error("Exam type is already active");

        if (examTypeRepository.existsByNameIgnoreCaseAndIsActiveTrueAndIdNot(examType.getName(), id)) {
            return ApiResponse.error("An active exam type with the name '" + examType.getName() + "' already exists. Rename before reactivating");
        }
        examType.setIsActive(true);
        return ApiResponse.success("Exam type reactivated successfully", examTypeRepository.save(examType));
    }

    @Override
    public ApiResponse<Void> delete(Integer id) {
        ExamType examType = examTypeRepository.findById(id).orElse(null);
        if (examType == null) return ApiResponse.error("Exam type not found");

        if (!examType.getIsActive()) return ApiResponse.error("Exam type is already inactive");

        examType.setIsActive(false);
        examTypeRepository.save(examType);
        return ApiResponse.success("Exam type deleted successfully", null);
    }
}