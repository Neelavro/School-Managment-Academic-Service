package com.example.academic_service.service.impl;

import com.example.academic_service.entity.AcademicYear;
import com.example.academic_service.repository.AcademicYearRepository;
import com.example.academic_service.service.AcademicYearService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AcademicYearServiceImpl implements AcademicYearService {

    @PersistenceContext
    private EntityManager entityManager;

    private final AcademicYearRepository academicYearRepository;

    public AcademicYearServiceImpl(AcademicYearRepository academicYearRepository) {
        this.academicYearRepository = academicYearRepository;
    }

    @Override
    @Transactional
    public void migrateAcademicYear(AcademicYear request) {
        entityManager.createNativeQuery(
                        "INSERT INTO academic_year (id, year_name, is_active) VALUES (:id, :yearName, :isActive)"
                )
                .setParameter("id", request.getId())
                .setParameter("yearName", request.getYearName())
                .setParameter("isActive", request.getIsActive())
                .executeUpdate();
    }

    @Override
    public AcademicYear createAcademicYear(AcademicYear academicYear) {
        academicYear.setIsActive(false);
        return academicYearRepository.save(academicYear);
    }

    @Override
    public List<AcademicYear> getAllAcademicYears() {
        return academicYearRepository.findAll();
    }

    @Override
    public AcademicYear getCurrentAcademicYear() {
        return academicYearRepository.findFirstByIsActiveTrue()
                .orElseThrow(() -> new RuntimeException("No active academic year set"));
    }

    @Override
    @Transactional
    public AcademicYear activateAcademicYear(Integer id) {
        AcademicYear year = academicYearRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Academic year not found"));
        academicYearRepository.deactivateAll();
        year.setIsActive(true);
        return academicYearRepository.save(year);
    }

    @Override
    public AcademicYear getAcademicYearById(Integer id) {
        return academicYearRepository.findById(id).orElse(null);
    }

    @Override
    public AcademicYear updateAcademicYear(Integer id, AcademicYear academicYear) {
        AcademicYear existing = getAcademicYearById(id);
        if (existing == null) return null;
        existing.setYearName(academicYear.getYearName());
        return academicYearRepository.save(existing);
    }

    @Override
    public void deleteAcademicYear(Integer id) {
        academicYearRepository.deleteById(id);
    }
}
