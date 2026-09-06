package com.example.academic_service.repository;

import com.example.academic_service.entity.ResultPublication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ResultPublicationRepository extends JpaRepository<ResultPublication, Integer> {

    List<ResultPublication> findByExamRoutine_Id(Integer routineId);

    Optional<ResultPublication> findByExamRoutine_IdAndStudentClass_Id(Integer routineId, Integer classId);

    @Query("SELECT rp.examRoutine.id FROM ResultPublication rp " +
           "WHERE rp.examRoutine.id IN :ids AND rp.studentClass.id = :classId AND rp.published = true")
    Set<Integer> findPublishedRoutineIdsForClass(@Param("ids") Collection<Integer> ids,
                                                 @Param("classId") Integer classId);
}
