package com.example.academic_service.repository;

import com.example.academic_service.entity.TeacherSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, Long> {

    List<TeacherSubject> findByAcademicYear_Id(Integer academicYearId);

    List<TeacherSubject> findByStaff_IdAndAcademicYear_Id(Long staffId, Integer academicYearId);

    List<TeacherSubject> findByAcademicYear_IdAndClassEntity_IdAndSubject_Id(Integer academicYearId, Integer classId, Integer subjectId);

    boolean existsByStaff_IdAndAcademicYear_IdAndClassEntity_IdAndSubject_Id(
            Long staffId, Integer academicYearId, Integer classId, Integer subjectId);
}
