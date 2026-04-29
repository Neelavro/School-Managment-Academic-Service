package com.example.academic_service.repository;

import com.example.academic_service.entity.StudentImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentImageRepository extends JpaRepository<StudentImage, Long> {

    Optional<StudentImage> findByStudentId(Long studentId);

    Optional<StudentImage> findByStudentIdAndIsActiveTrue(Long studentId);

    List<StudentImage> findAllByIsActiveTrue();

    default void softDelete(StudentImage image) {
        image.setIsActive(false);
        save(image);
    }
}
