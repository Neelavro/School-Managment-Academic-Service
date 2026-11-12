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

import com.example.academic_service.dto.SubSubjectBulkRequest;
import com.example.academic_service.dto.SubSubjectClassesRequest;
import com.example.academic_service.entity.SubSubjectClass;
import com.example.academic_service.repository.SubSubjectClassRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final SubSubjectClassRepository subSubjectClassRepository;
    private final ClassRepository classRepository;

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

    // The sub-subject endpoints throw (HTTP 400) instead of ApiResponse.error, which is HTTP 200 and reads as success.
    @Override
    @Transactional
    public ApiResponse<List<Subject>> createSubSubjects(SubSubjectBulkRequest request) {
        if (request.getParentSubjectId() == null) throw new RuntimeException("Choose the parent subject");
        String parentError = parentError(null, request.getParentSubjectId());
        if (parentError != null) throw new RuntimeException(parentError);
        List<SubSubjectBulkRequest.Item> items = request.getSubSubjects() == null ? List.of()
                : request.getSubSubjects().stream().filter(i -> i.getName() != null && !i.getName().isBlank()).toList();
        if (items.isEmpty()) throw new RuntimeException("Add at least one sub-subject name");
        // All names and codes are checked before anything is saved.
        Set<String> names = new HashSet<>(), codes = new HashSet<>();
        for (SubSubjectBulkRequest.Item i : items) {
            String name = i.getName().trim();
            String code = i.getCode() != null && !i.getCode().isBlank() ? i.getCode().trim() : null;
            if (!names.add(name.toLowerCase())) throw new RuntimeException("'" + name + "' is listed twice");
            if (subjectRepository.existsByNameIgnoreCase(name)) throw new RuntimeException("A subject named '" + name + "' already exists");
            if (code != null && !codes.add(code.toLowerCase())) throw new RuntimeException("Code '" + code + "' is listed twice");
            if (code != null && subjectRepository.existsByCodeIgnoreCase(code)) throw new RuntimeException("A subject with code '" + code + "' already exists");
        }
        String classError = classesError(request.getClassIds());
        if (classError != null) throw new RuntimeException(classError);

        List<Subject> created = new ArrayList<>();
        for (SubSubjectBulkRequest.Item i : items) {
            Subject s = new Subject();
            s.setName(i.getName().trim());
            s.setCode(i.getCode() != null && !i.getCode().isBlank() ? i.getCode().trim() : null);
            s.setParentSubjectId(request.getParentSubjectId());
            created.add(subjectRepository.save(s));
        }
        if (request.getClassIds() != null)
            assign(created.stream().map(Subject::getId).toList(), request.getClassIds(), false);
        return ApiResponse.success(created.size() + " sub-subject(s) created", created);
    }

    @Override
    public ApiResponse<List<SubSubjectClass>> subSubjectClasses() {
        return ApiResponse.success("Sub-subject classes fetched", subSubjectClassRepository.findAll());
    }

    @Override
    @Transactional
    public ApiResponse<List<SubSubjectClass>> assignSubSubjectClasses(SubSubjectClassesRequest request) {
        List<Integer> ids = request.getSubSubjectIds() == null ? List.of() : request.getSubSubjectIds();
        if (ids.isEmpty()) throw new RuntimeException("Choose at least one sub-subject");
        List<Integer> classIds = request.getClassIds() == null ? List.of() : request.getClassIds();
        boolean replace = Boolean.TRUE.equals(request.getReplace());
        if (classIds.isEmpty() && !replace) throw new RuntimeException("Choose at least one class");
        for (Subject s : subjectRepository.findAllById(ids))
            if (s.getParentSubjectId() == null) throw new RuntimeException("'" + s.getName() + "' isn't a sub-subject");
        if (subjectRepository.findAllById(ids).size() != new HashSet<>(ids).size()) throw new RuntimeException("Sub-subject not found");
        String classError = classesError(classIds);
        if (classError != null) throw new RuntimeException(classError);
        assign(ids, classIds, replace);
        return ApiResponse.success("Classes saved", subSubjectClassRepository.findBySubSubjectIdIn(ids));
    }

    private String classesError(List<Integer> classIds) {
        if (classIds == null || classIds.isEmpty()) return null;
        Set<Integer> unique = new HashSet<>(classIds);
        return classRepository.findAllById(unique).size() == unique.size() ? null : "Class not found";
    }

    private void assign(List<Integer> subSubjectIds, List<Integer> classIds, boolean replace) {
        Set<Integer> wanted = new HashSet<>(classIds);
        for (Integer id : new LinkedHashSet<>(subSubjectIds)) {
            List<SubSubjectClass> current = subSubjectClassRepository.findBySubSubjectId(id);
            Set<Integer> have = new HashSet<>();
            for (SubSubjectClass c : current) {
                if (replace && !wanted.contains(c.getClassId())) subSubjectClassRepository.delete(c);
                else have.add(c.getClassId());
            }
            for (Integer classId : wanted) {
                if (have.contains(classId)) continue;
                SubSubjectClass a = new SubSubjectClass();
                a.setSubSubjectId(id);
                a.setClassId(classId);
                subSubjectClassRepository.save(a);
            }
        }
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