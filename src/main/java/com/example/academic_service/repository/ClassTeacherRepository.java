package com.example.academic_service.repository;

import com.example.academic_service.entity.ClassTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassTeacherRepository extends JpaRepository<ClassTeacher, Long> {

    List<ClassTeacher> findAllByAcademicYearId(Integer academicYearId);

    Optional<ClassTeacher> findBySectionIdAndAcademicYearId(Long sectionId, Integer academicYearId);

    /** A teacher can be class teacher of several registers in a year (user, 2026-10-18). */
    List<ClassTeacher> findAllByStaffIdAndAcademicYearIdOrderByIdAsc(Long staffId, Integer academicYearId);

    boolean existsBySectionIdAndAcademicYearId(Long sectionId, Integer academicYearId);

    /** A class-teacher for a class + gender section without sections (section null). */
    boolean existsByStudentClassIdAndGenderSectionIdAndSectionIsNullAndAcademicYearId(Integer classId, Integer genderSectionId, Integer academicYearId);
}
