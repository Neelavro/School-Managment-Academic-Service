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

    private final GenderSectionRepository genderSectionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    GenderSectionServiceImpl(GenderSectionRepository genderSectionRepository) {
        this.genderSectionRepository = genderSectionRepository;
    }

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

    @Override
    public List<GenderSection> getAllGenderSections() {
        return genderSectionRepository.findAll();
    }

    @Override
    public GenderSection create(String genderName) {
        if (genderSectionRepository.existsByGenderNameIgnoreCase(genderName.trim())) {
            throw new RuntimeException("Gender section '" + genderName + "' already exists");
        }
        GenderSection gs = new GenderSection();
        gs.setGenderName(genderName.trim());
        return genderSectionRepository.save(gs);
    }

    @Override
    public GenderSection update(Integer id, String genderName) {
        GenderSection gs = genderSectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gender section not found"));
        if (genderSectionRepository.existsByGenderNameIgnoreCaseAndIdNot(genderName.trim(), id)) {
            throw new RuntimeException("Gender section '" + genderName + "' already exists");
        }
        gs.setGenderName(genderName.trim());
        return genderSectionRepository.save(gs);
    }

    @Override
    public void delete(Integer id) {
        GenderSection gs = genderSectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gender section not found"));
        genderSectionRepository.delete(gs);
    }
}
