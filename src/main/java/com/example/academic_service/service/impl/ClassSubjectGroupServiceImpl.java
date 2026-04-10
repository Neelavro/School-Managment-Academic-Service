package com.example.academic_service.service.impl;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.ClassSubjectGroupRequestDto;
import com.example.academic_service.entity.ClassSubjectGroup;
import com.example.academic_service.entity.Class;
import com.example.academic_service.entity.StudentGroup;
import com.example.academic_service.entity.Subject;
import com.example.academic_service.repository.ClassRepository;
import com.example.academic_service.repository.ClassSubjectGroupRepository;
import com.example.academic_service.repository.StudentGroupRepository;
import com.example.academic_service.repository.SubjectRepository;
import com.example.academic_service.service.ClassSubjectGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassSubjectGroupServiceImpl implements ClassSubjectGroupService {

    private final ClassSubjectGroupRepository repository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final StudentGroupRepository studentGroupRepository;

    @Override
    public ApiResponse<ClassSubjectGroup> assign(ClassSubjectGroupRequestDto dto) {
        Class studentClass = classRepository.findById(dto.getClassId()).orElse(null);
        if (studentClass == null) return ApiResponse.error("Class not found");
        if (!studentClass.getIsActive())
            return ApiResponse.error("Cannot assign to an inactive class");

        Subject subject = subjectRepository.findById(dto.getSubjectId()).orElse(null);
        if (subject == null) return ApiResponse.error("Subject not found");
        if (!subject.getIsActive())
            return ApiResponse.error("Cannot assign an inactive subject");

        // Validate group belongs to class if provided
        if (dto.getStudentGroupId() != null) {
            boolean groupBelongsToClass = studentClass.getStudentGroups()
                    .stream()
                    .anyMatch(g -> g.getId().equals(dto.getStudentGroupId()));
            if (!groupBelongsToClass) {
                return ApiResponse.error("This group does not belong to the selected class");
            }
        }

        // Duplicate check
        if (dto.getStudentGroupId() != null) {
            if (repository.existsByStudentClassIdAndSubjectIdAndStudentGroupIdAndIsActiveTrue(
                    dto.getClassId(), dto.getSubjectId(), dto.getStudentGroupId())) {
                return ApiResponse.error(
                        "This subject is already assigned to this class and group");
            }
        } else {
            if (repository.existsByStudentClassIdAndSubjectIdAndStudentGroupIsNullAndIsActiveTrue(
                    dto.getClassId(), dto.getSubjectId())) {
                return ApiResponse.error(
                        "This subject is already assigned to this class without a group");
            }
        }

        StudentGroup studentGroup = null;
        if (dto.getStudentGroupId() != null) {
            studentGroup = studentGroupRepository.findById(dto.getStudentGroupId()).orElse(null);
            if (studentGroup == null) return ApiResponse.error("Student group not found");
        }

        ClassSubjectGroup assignment = new ClassSubjectGroup();
        assignment.setStudentClass(studentClass);
        assignment.setSubject(subject);
        assignment.setStudentGroup(studentGroup);

        return ApiResponse.success("Subject assigned successfully", repository.save(assignment));
    }

    @Override
    public ApiResponse<List<ClassSubjectGroup>> getByClass(Integer classId) {
        Class studentClass = classRepository.findById(classId).orElse(null);
        if (studentClass == null) return ApiResponse.error("Class not found");
        return ApiResponse.success("Assignments fetched successfully",
                repository.findByStudentClassIdAndIsActiveTrue(classId));
    }

    @Override
    public ApiResponse<List<ClassSubjectGroup>> getByClassAndGroup(
            Integer classId, Integer studentGroupId) {
        Class studentClass = classRepository.findById(classId).orElse(null);
        if (studentClass == null) return ApiResponse.error("Class not found");

        List<ClassSubjectGroup> results;
        if (studentGroupId == null) {
            results = repository.findByStudentClassIdAndStudentGroupIsNullAndIsActiveTrue(classId);
        } else {
            results = repository.findByStudentClassIdAndStudentGroupIdAndIsActiveTrue(
                    classId, studentGroupId);
        }
        return ApiResponse.success("Assignments fetched successfully", results);
    }
    @Override
    public ApiResponse<List<ClassSubjectGroup>> getBySubject(Integer subjectId) {
        Subject subject = subjectRepository.findById(subjectId).orElse(null);
        if (subject == null) return ApiResponse.error("Subject not found");
        return ApiResponse.success("Assignments fetched successfully",
                repository.findBySubjectIdAndIsActiveTrue(subjectId));
    }

    @Override
    public ApiResponse<Void> remove(Integer id) {
        ClassSubjectGroup assignment = repository.findById(id).orElse(null);
        if (assignment == null) return ApiResponse.error("Assignment not found");
        repository.delete(assignment);
        return ApiResponse.success("Assignment removed successfully", null);
    }
}