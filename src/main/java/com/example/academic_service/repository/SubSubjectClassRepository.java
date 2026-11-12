package com.example.academic_service.repository;

import com.example.academic_service.entity.SubSubjectClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SubSubjectClassRepository extends JpaRepository<SubSubjectClass, Integer> {
    List<SubSubjectClass> findByClassId(Integer classId);
    List<SubSubjectClass> findBySubSubjectId(Integer subSubjectId);
    List<SubSubjectClass> findBySubSubjectIdIn(Collection<Integer> subSubjectIds);
    boolean existsBySubSubjectIdAndClassId(Integer subSubjectId, Integer classId);
}
