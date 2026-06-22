package com.example.academic_service.repository;

import com.example.academic_service.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long>,
        JpaSpecificationExecutor<Enrollment> {

    // Check duplicate enrollment in same academic year
    boolean existsByStudentSystemIdAndAcademicYearId(String studentSystemId, Integer academicYearId);

    // Find a specific student's enrollment in a given academic year
    Optional<Enrollment> findByStudentSystemIdAndAcademicYearId(String studentSystemId, Integer academicYearId);

    // Find all enrollments for a student across all years
    List<Enrollment> findByStudentSystemId(String studentSystemId);

    // Find all enrollments for a student that are active
    List<Enrollment> findByStudentSystemIdAndIsActive(String studentSystemId, Boolean isActive);

    @Query("SELECT e FROM Enrollment e WHERE e.studentSystemId = :studentSystemId AND e.isActive = true AND e.academicYear.isActive = true")
    java.util.Optional<Enrollment> findActiveEnrollmentByStudentSystemId(@Param("studentSystemId") String studentSystemId);
    @Query("""
SELECT e FROM Enrollment e
WHERE e.studentClass.id = :classId
AND e.isActive = true
AND (:academicYearId IS NULL OR e.academicYear.id = :academicYearId)
AND (:shiftId IS NULL OR e.shift.id = :shiftId)
AND (:genderSectionId IS NULL OR e.genderSection.id = :genderSectionId)
AND (:sectionId IS NULL OR e.section.id = :sectionId)
AND (:groupId IS NULL OR (e.studentGroup IS NOT NULL AND e.studentGroup.id = :groupId))
AND (:startRoll IS NULL OR e.classRoll >= :startRoll)
AND (:endRoll IS NULL OR e.classRoll <= :endRoll)
""")
    List<Enrollment> findAllByClassIdAndFilters(
            @Param("classId") Integer classId,
            @Param("academicYearId") Integer academicYearId,
            @Param("shiftId") Integer shiftId,
            @Param("genderSectionId") Integer genderSectionId,
            @Param("sectionId") Long sectionId,
            @Param("groupId") Integer groupId,
            @Param("startRoll") Integer startRoll,
            @Param("endRoll") Integer endRoll
    );

    List<Enrollment> findByAcademicYearIdAndIsActiveTrue(Integer academicYearId);

    /**
     * Used by Accounting → Invoice generation.
     * Returns only active enrollments with both a class and academic year assigned.
     * Optional filters: academic year, set of class ids.
     * Pass classIdsEmpty=true and an empty (but non-null) list when no class filter is wanted —
     * JPQL does not allow empty IN clauses, so we gate it with a boolean.
     */
    @Query("""
        SELECT e FROM Enrollment e
        WHERE e.isActive = true
          AND e.studentClass IS NOT NULL
          AND (:academicYearId IS NULL OR e.academicYear.id = :academicYearId)
          AND (:classIdsEmpty = true OR e.studentClass.id IN :classIds)
    """)
    List<Enrollment> findInvoiceCandidates(
            @Param("academicYearId") Integer academicYearId,
            @Param("classIdsEmpty") boolean classIdsEmpty,
            @Param("classIds") List<Integer> classIds);

    @Query("""
SELECT e FROM Enrollment e
WHERE e.section.id = :sectionId
AND e.academicYear.id = :academicYearId
AND e.isActive = true
ORDER BY e.classRoll ASC
""")
    List<Enrollment> findBySectionAndYear(
            @Param("sectionId") Long sectionId,
            @Param("academicYearId") Integer academicYearId
    );
}