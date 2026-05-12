package com.example.academic_service.repository;

import com.example.academic_service.entity.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {
    Optional<TeacherProfile> findByStaffId(Long staffId);
    boolean existsByStaffId(Long staffId);
}
