package com.example.academic_service.repository;

import com.example.academic_service.entity.StudentMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentMarkRepository extends JpaRepository<StudentMark, Long> {

    Optional<StudentMark> findByEnrollmentIdAndRoutineIdAndSubjectIdAndExamComponentId(
            Long enrollmentId, Integer routineId, Integer subjectId, Integer examComponentId);

    List<StudentMark> findAllByRoutineIdAndSubjectId(Integer routineId, Integer subjectId);

    List<StudentMark> findAllByEnrollmentIdInAndRoutineIdAndSubjectId(
            List<Long> enrollmentIds, Integer routineId, Integer subjectId);

    boolean existsBySubjectIdAndExamComponentIdIn(Integer subjectId, List<Integer> examComponentIds);

    @Query("SELECT CASE WHEN COUNT(sm) > 0 THEN TRUE ELSE FALSE END FROM StudentMark sm " +
           "WHERE sm.subjectId = :subjectId AND sm.examComponent.id IN :componentIds " +
           "AND sm.enrollmentId IN (SELECT e.id FROM Enrollment e WHERE e.studentClass.id = :classId)")
    boolean existsBySubjectIdAndExamComponentIdInAndClassId(
            @Param("subjectId") Integer subjectId,
            @Param("componentIds") List<Integer> componentIds,
            @Param("classId") Integer classId);

    void deleteBySubjectIdAndExamComponentIdIn(Integer subjectId, List<Integer> examComponentIds);

    @Modifying
    @Query("DELETE FROM StudentMark sm WHERE sm.subjectId = :subjectId " +
           "AND sm.examComponent.id IN :componentIds " +
           "AND sm.enrollmentId IN (SELECT e.id FROM Enrollment e WHERE e.studentClass.id = :classId)")
    void deleteBySubjectIdAndExamComponentIdInAndClassId(
            @Param("subjectId") Integer subjectId,
            @Param("componentIds") List<Integer> componentIds,
            @Param("classId") Integer classId);

    @Query("SELECT sm FROM StudentMark sm WHERE sm.enrollmentId IN :enrollmentIds AND sm.routineId = :routineId")
    List<StudentMark> findAllByEnrollmentIdsAndRoutineId(
            @Param("enrollmentIds") List<Long> enrollmentIds,
            @Param("routineId") Integer routineId);

    @Query("SELECT sm FROM StudentMark sm WHERE sm.enrollmentId IN :enrollmentIds AND sm.routineId IN :routineIds")
    List<StudentMark> findAllByEnrollmentIdsAndRoutineIds(
            @Param("enrollmentIds") List<Long> enrollmentIds,
            @Param("routineIds") List<Integer> routineIds);
}
