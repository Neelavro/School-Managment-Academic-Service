package com.example.academic_service.repository;

import com.example.academic_service.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Integer> {
    List<Section> findAllByClassEntityIdAndGenderSectionIdAndIsActiveTrue(
            Integer classId,
            Integer genderSectionId
    );}
