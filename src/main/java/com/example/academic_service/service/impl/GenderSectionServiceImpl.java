package com.example.academic_service.service.impl;

import com.example.academic_service.entity.GenderSection;
import com.example.academic_service.repository.GenderSectionRepository;
import com.example.academic_service.service.GenderSectionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GenderSectionServiceImpl implements GenderSectionService {

    GenderSectionRepository genderSectionRepository;

    @PersistenceContext
    private EntityManager entityManager;
    @Override
    @Transactional
    public void migrateGenderSection(GenderSection request) {
        entityManager.createNativeQuery(
                        "INSERT INTO gender_section (id, gender_name) VALUES (:id, :genderName)"
                )
                .setParameter("id", request.getId())
                .setParameter("genderName", request.getGenderName())
                .executeUpdate();
    }

    GenderSectionServiceImpl(GenderSectionRepository genderSectionRepository){
        this.genderSectionRepository = genderSectionRepository;
    }

    public List<GenderSection> getAllGenderSections(){
        return genderSectionRepository.findAll();
    }

}
