package com.example.academic_service.repository;

import com.example.academic_service.entity.*;
import com.example.academic_service.entity.Class;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarkingStructureRepository extends JpaRepository<MarkingStructure, Integer> {

    Optional<MarkingStructure> findByIdAndDeletedAtIsNull(Integer id);

    boolean existsByExamTypeAndExamClassAndSubjectAndGroupAndDeletedAtIsNull(
            ExamType examType, Class examClass, Subject subject, StudentGroup group);

    @Query(value = "SELECT * FROM marking_structure m WHERE " +
            "(:examTypeId IS NULL OR m.exam_type_id = :examTypeId) AND " +
            "(:classId IS NULL OR m.class_id = :classId) AND " +
            "(:subjectId IS NULL OR m.subject_id = :subjectId) AND " +
            "(:groupId IS NULL OR m.group_id = :groupId) AND " +
            "m.deleted_at IS NULL",
            nativeQuery = true)
    List<MarkingStructure> findAllByFiltersAndDeletedAtIsNull(
            @Param("examTypeId") Integer examTypeId,
            @Param("classId") Integer classId,
            @Param("subjectId") Integer subjectId,
            @Param("groupId") Integer groupId);

    @Query(value = "SELECT * FROM marking_structure m WHERE " +
            "m.exam_type_id = :examTypeId AND " +
            "m.class_id = :classId AND " +
            "m.subject_id = :subjectId AND " +
            "m.group_id IS NULL AND " +
            "m.deleted_at IS NULL",
            nativeQuery = true)
    List<MarkingStructure> findClassWideAndDeletedAtIsNull(
            @Param("examTypeId") Integer examTypeId,
            @Param("classId") Integer classId,
            @Param("subjectId") Integer subjectId);
}