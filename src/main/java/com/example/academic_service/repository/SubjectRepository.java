package com.example.academic_service.repository;

import com.example.academic_service.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Integer> {
    List<Subject> findByIsActiveTrue();
    List<Subject> findByIsActiveFalse();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Integer id);
    boolean existsByNameIgnoreCaseAndIsActiveTrueAndIdNot(String name, Integer id);
    boolean existsByCodeIgnoreCaseAndIsActiveTrueAndIdNot(String code, Integer id);
    Optional<Subject> findByIdAndIsActiveTrue(Integer id);
}