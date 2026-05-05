package com.example.academic_service.repository;

import com.example.academic_service.entity.StudentMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentMarkRepository extends JpaRepository<StudentMark, Long> {

    Optional<StudentMark> findByEnrollmentIdAndRoutineIdAndSubjectIdAndExamComponentId(
            Long enrollmentId, Integer routineId, Integer subjectId, Integer examComponentId);

    List<StudentMark> findAllByRoutineIdAndSubjectIdAndDeletedAtIsNull(Integer routineId, Integer subjectId);

    List<StudentMark> findAllByEnrollmentIdInAndRoutineIdAndSubjectIdAndDeletedAtIsNull(
            List<Long> enrollmentIds, Integer routineId, Integer subjectId);

    @Query("SELECT sm FROM StudentMark sm WHERE sm.enrollmentId IN :enrollmentIds AND sm.routineId = :routineId AND sm.deletedAt IS NULL")
    List<StudentMark> findAllByEnrollmentIdsAndRoutineId(
            @Param("enrollmentIds") List<Long> enrollmentIds,
            @Param("routineId") Integer routineId);

    @Query("SELECT sm FROM StudentMark sm WHERE sm.enrollmentId IN :enrollmentIds AND sm.routineId IN :routineIds AND sm.deletedAt IS NULL")
    List<StudentMark> findAllByEnrollmentIdsAndRoutineIds(
            @Param("enrollmentIds") List<Long> enrollmentIds,
            @Param("routineIds") List<Integer> routineIds);
}
