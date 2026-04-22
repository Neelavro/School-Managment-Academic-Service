package com.example.academic_service.service;

import com.example.academic_service.dto.marking_dtos.*;
import com.example.academic_service.entity.ExamComponent;
import com.example.academic_service.repository.ExamComponentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExamComponentService {

    private final ExamComponentRepository examComponentRepository;

    public Map<String, Object> create(ExamComponentRequest request) {
        if (examComponentRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(request.getName())) {
            throw new RuntimeException("Exam component with name '" + request.getName() + "' already exists");
        }
        ExamComponent component = new ExamComponent();
        component.setName(request.getName());
        component.setOrderIndex(request.getOrderIndex());
        ExamComponentResponse response = toResponse(examComponentRepository.save(component));
        return Map.of("message", "Exam component created successfully", "data", response);
    }

    public Map<String, Object> getAll() {
        List<ExamComponentResponse> list = examComponentRepository.findAllByDeletedAtIsNullOrderByOrderIndexAsc()
                .stream().map(this::toResponse).toList();
        return Map.of("message", "Exam components fetched successfully", "data", list);
    }

    public Map<String, Object> update(Integer id, ExamComponentRequest request) {
        ExamComponent component = examComponentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Exam component not found with id: " + id));

        if (request.getName() != null) {
            boolean nameConflict = examComponentRepository.existsByNameIgnoreCaseAndDeletedAtIsNullAndIdNot(
                    request.getName(), id);
            if (nameConflict) {
                throw new RuntimeException("Exam component with name '" + request.getName() + "' already exists");
            }
            component.setName(request.getName());
        }

        if (request.getOrderIndex() != null) {
            component.setOrderIndex(request.getOrderIndex());
        }

        ExamComponentResponse response = toResponse(examComponentRepository.save(component));
        return Map.of("message", "Exam component updated successfully", "data", response);
    }

    public Map<String, String> delete(Integer id) {
        ExamComponent component = examComponentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Exam component not found with id: " + id));
        component.setDeletedAt(LocalDateTime.now());
        component.setIsActive(false);
        examComponentRepository.save(component);
        return Map.of("message", "Exam component '" + component.getName() + "' deleted successfully");
    }

    private ExamComponentResponse toResponse(ExamComponent c) {
        ExamComponentResponse res = new ExamComponentResponse();
        res.setId(c.getId());
        res.setName(c.getName());
        res.setOrderIndex(c.getOrderIndex());
        res.setIsActive(c.getIsActive());
        return res;
    }
}