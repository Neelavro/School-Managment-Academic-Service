package com.example.academic_service.service;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.ClassSubjectGroupRequestDto;
import com.example.academic_service.entity.ClassSubjectGroup;

import java.util.List;

public interface ClassSubjectGroupService {
    ApiResponse<List<ClassSubjectGroup>> getBySubject(Integer subjectId);

    ApiResponse<ClassSubjectGroup> assign(ClassSubjectGroupRequestDto dto);
    ApiResponse<List<ClassSubjectGroup>> getByClass(Integer classId);
    ApiResponse<List<ClassSubjectGroup>> getByClassAndGroup(Integer classId, Integer studentGroupId);
    ApiResponse<ClassSubjectGroup> update(Integer id, ClassSubjectGroupRequestDto dto);
    ApiResponse<Void> remove(Integer id);
}