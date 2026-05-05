package com.example.academic_service.service;

import com.example.academic_service.entity.AcademicYear;

import java.util.List;

public interface AcademicYearService {

    void migrateAcademicYear(AcademicYear request);

    AcademicYear createAcademicYear(AcademicYear academicYear);

    List<AcademicYear> getAllAcademicYears();

    AcademicYear getCurrentAcademicYear();

    AcademicYear activateAcademicYear(Integer id);

    AcademicYear getAcademicYearById(Integer id);

    AcademicYear updateAcademicYear(Integer id, AcademicYear academicYear);

    void deleteAcademicYear(Integer id);
}
