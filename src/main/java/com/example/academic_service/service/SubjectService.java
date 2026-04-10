package com.example.academic_service.service;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.SubjectRequestDto;
import com.example.academic_service.entity.Subject;

import java.util.List;

public interface SubjectService {
    ApiResponse<Subject> create(SubjectRequestDto dto);
    ApiResponse<Subject> update(Integer id, SubjectRequestDto dto);
    ApiResponse<Subject> getById(Integer id);
    ApiResponse<List<Subject>> getAll(Boolean active);
    ApiResponse<Subject> reactivate(Integer id);
    ApiResponse<Void> delete(Integer id);
}