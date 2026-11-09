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
        String parentError = parentError(null, dto.getParentSubjectId());
        if (parentError != null) return ApiResponse.error(parentError);
        Subject subject = new Subject();
        subject.setName(dto.getName());
        subject.setCode(dto.getCode());
        subject.setParentSubjectId(dto.getParentSubjectId());
        if (dto.getOrderIndex() != null) subject.setOrderIndex(dto.getOrderIndex());
        return ApiResponse.success(dto.getParentSubjectId() != null ? "Sub-subject created successfully" : "Subject created successfully",
                subjectRepository.save(subject));
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
        // No parent in the request keeps the current one (reorder and older clients don't send it).
        if (dto.getParentSubjectId() != null && !dto.getParentSubjectId().equals(subject.getParentSubjectId())) {
            String parentError = parentError(id, dto.getParentSubjectId());
            if (parentError != null) return ApiResponse.error(parentError);
            subject.setParentSubjectId(dto.getParentSubjectId());
        }
        subject.setName(dto.getName());
        subject.setCode(dto.getCode());
        if (dto.getOrderIndex() != null) subject.setOrderIndex(dto.getOrderIndex());
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
        if (subject.getParentSubjectId() != null
                && subjectRepository.findByIdAndIsActiveTrue(subject.getParentSubjectId()).isEmpty()) {
            return ApiResponse.error("Reactivate the parent subject first");
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
        // A parent's sub-subjects go with it (they mean nothing on their own).
        List<Subject> children = subjectRepository.findByParentSubjectIdAndIsActiveTrue(id);
        children.forEach(c -> c.setIsActive(false));
        subjectRepository.saveAll(children);
        return ApiResponse.success(children.isEmpty() ? "Subject deleted successfully"
                : "Subject and its " + children.size() + " sub-subject(s) deleted successfully", null);
    }

    /**
     * Sub-subject rules: the parent must be an active, ordinary subject (one level only), not the subject itself,
     * and a subject that already has sub-subjects can't become one.
     */
    private String parentError(Integer subjectId, Integer parentId) {
        if (parentId == null) return null;
        if (parentId.equals(subjectId)) return "A subject can't be its own parent";
        Subject parent = subjectRepository.findByIdAndIsActiveTrue(parentId).orElse(null);
        if (parent == null) return "Parent subject not found";
        if (parent.getParentSubjectId() != null) return "'" + parent.getName() + "' is itself a sub-subject; sub-subjects can't have their own";
        if (subjectId != null && subjectRepository.existsByParentSubjectIdAndIsActiveTrue(subjectId))
            return "This subject has sub-subjects, so it can't become a sub-subject";
        return null;
    }
}