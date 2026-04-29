package com.example.academic_service.repository;

import com.example.academic_service.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
    boolean existsByStudentSystemId(String studentSystemId);
    Optional<Student> findByStudentSystemId(String studentSystemId);

    @Query("SELECT s.studentSystemId FROM Student s WHERE s.studentSystemId LIKE CONCAT(:year, '%') ORDER BY s.studentSystemId DESC LIMIT 1")
    String findMaxStudentSystemIdByYear(@Param("year") String year);
}
