package com.example.academic_service.repository;

import com.example.academic_service.entity.MarkingStructure;
import com.example.academic_service.entity.MarkingStructureComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarkingStructureComponentRepository extends JpaRepository<MarkingStructureComponent, Integer> {

    List<MarkingStructureComponent> findAllByMarkingStructureAndDeletedAtIsNull(MarkingStructure markingStructure);
}