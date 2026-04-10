package com.example.academic_service.repository;

import com.example.academic_service.entity.ClassSubjectGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassSubjectGroupRepository extends JpaRepository<ClassSubjectGroup, Integer> {

    List<ClassSubjectGroup> findByStudentClassIdAndIsActiveTrue(Integer classId);

    List<ClassSubjectGroup> findByStudentClassIdAndStudentGroupIdAndIsActiveTrue(
            Integer classId, Integer studentGroupId);

    List<ClassSubjectGroup> findByStudentClassIdAndStudentGroupIsNullAndIsActiveTrue(
            Integer classId);

    boolean existsByStudentClassIdAndSubjectIdAndStudentGroupIdAndIsActiveTrue(
            Integer classId, Integer subjectId, Integer studentGroupId);

    boolean existsByStudentClassIdAndSubjectIdAndStudentGroupIsNullAndIsActiveTrue(
            Integer classId, Integer subjectId);
    List<ClassSubjectGroup> findBySubjectIdAndIsActiveTrue(Integer subjectId);

    @Query("""
        SELECT csg FROM ClassSubjectGroup csg
        WHERE csg.studentClass.id = :classId
        AND csg.isActive = true
        AND (csg.studentGroup.id = :studentGroupId OR csg.studentGroup IS NULL)
    """)
    List<ClassSubjectGroup> findSubjectsForStudent(
            @Param("classId") Integer classId,
            @Param("studentGroupId") Integer studentGroupId);
}