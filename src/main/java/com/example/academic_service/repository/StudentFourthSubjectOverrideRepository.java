package com.example.academic_service.repository;

import com.example.academic_service.entity.StudentFourthSubjectOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentFourthSubjectOverrideRepository extends JpaRepository<StudentFourthSubjectOverride, Long> {
    Optional<StudentFourthSubjectOverride> findByEnrollmentId(Long enrollmentId);
    List<StudentFourthSubjectOverride> findByEnrollmentIdIn(List<Long> enrollmentIds);
    void deleteByEnrollmentId(Long enrollmentId);
}
