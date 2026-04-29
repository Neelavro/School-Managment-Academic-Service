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

    Optional<StudentMark> findByEnrollmentIdAndExamSessionIdAndExamComponentId(
            Long enrollmentId, Integer examSessionId, Integer examComponentId);

    List<StudentMark> findAllByExamSessionIdAndDeletedAtIsNull(Integer examSessionId);

    List<StudentMark> findAllByEnrollmentIdInAndExamSessionIdAndDeletedAtIsNull(
            List<Long> enrollmentIds, Integer examSessionId);

    @Query("SELECT sm FROM StudentMark sm WHERE sm.enrollmentId IN :enrollmentIds AND sm.examSession.id IN :sessionIds AND sm.deletedAt IS NULL")
    List<StudentMark> findAllByEnrollmentIdsAndSessionIds(
            @Param("enrollmentIds") List<Long> enrollmentIds,
            @Param("sessionIds") List<Integer> sessionIds);
}