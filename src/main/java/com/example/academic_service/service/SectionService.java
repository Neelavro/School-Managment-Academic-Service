package com.example.academic_service.service;

import com.example.academic_service.entity.GenderSection;
import com.example.academic_service.entity.Section;

import java.util.List;

public interface SectionService {

    public void migrateSection(Section request);
    Section createSection(Section section);

    Section updateSection(Long id, Section section);

    Section getSectionById(Long id);

    List<Section> getAllActiveSections();

    List<Section> getSectionsByClassIdAndGenderSectionId(Integer classId, Integer genderSectionId); // ✅ NEW

    /** Active and inactive — for the Sections page, so an inactive section can be switched back on. */
    List<Section> getAllSectionsByClassIdAndGenderSectionId(Integer classId, Integer genderSectionId);

    void deleteSection(Long id); // soft delete
}
