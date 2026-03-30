package com.example.academic_service.service;

import com.example.academic_service.entity.GenderSection;

import java.util.List;

public interface GenderSectionService {
    public void migrateGenderSection(GenderSection request);
    List<GenderSection> getAllGenderSections();
}
