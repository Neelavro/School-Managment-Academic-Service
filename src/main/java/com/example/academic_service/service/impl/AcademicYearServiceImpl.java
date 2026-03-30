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
        AcademicYear year = new AcademicYear();
        return academicYearRepository.save(academicYear);
    }

    @Override
    public List<AcademicYear> getAllAcademicYears() {
        return academicYearRepository.findByIsActiveTrue();
    }

    @Override
    public AcademicYear getAcademicYearById(Integer id) {
        Optional<AcademicYear> year = academicYearRepository.findById(id);
        return year.orElse(null); // or throw custom exception
    }

    @Override
    public AcademicYear updateAcademicYear(Integer id, AcademicYear academicYear) {
        AcademicYear existing = getAcademicYearById(id);
        if (existing != null) {
            existing.setYearName(academicYear.getYearName());
            existing.setIsActive(academicYear.getIsActive());
            return academicYearRepository.save(existing);
        }
        return null; // or throw custom exception
    }

    @Override
    public void deleteAcademicYear(Integer id) {
        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Academic year not found"));

        academicYear.setIsActive(false);
        academicYearRepository.save(academicYear);
    }}
