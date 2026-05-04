package com.example.academic_service.repository;

import com.example.academic_service.entity.ExamClassRoomAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamClassRoomAssignmentRepository extends JpaRepository<ExamClassRoomAssignment, Integer> {

    List<ExamClassRoomAssignment> findByExamRoutineId(Integer examRoutineId);

    List<ExamClassRoomAssignment> findByExamRoutineIdAndExamClassId(Integer examRoutineId, Integer classId);

    @Modifying
    @Query("DELETE FROM ExamClassRoomAssignment a WHERE a.examRoutine.id = :routineId AND a.examClass.id = :classId")
    void deleteByExamRoutineIdAndExamClassId(@Param("routineId") Integer routineId, @Param("classId") Integer classId);
}
