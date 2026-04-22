package com.example.academic_service.repository;

import com.example.academic_service.entity.ExamComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamComponentRepository extends JpaRepository<ExamComponent, Integer> {

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNullAndIdNot(String name, Integer id);

    Optional<ExamComponent> findByIdAndDeletedAtIsNull(Integer id);

    List<ExamComponent> findAllByDeletedAtIsNullOrderByOrderIndexAsc();
}