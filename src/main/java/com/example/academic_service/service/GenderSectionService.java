package com.example.academic_service.service;

import com.example.academic_service.entity.GenderSection;

import java.util.List;

public interface GenderSectionService {
    void migrateGenderSection(GenderSection request);
    List<GenderSection> getAllGenderSections();
    GenderSection create(String genderName);
    GenderSection update(Integer id, String genderName);
    void delete(Integer id);
}
