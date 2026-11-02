package com.example.academic_service.repository;

import com.example.academic_service.entity.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {
    Optional<TeacherProfile> findByStaffId(Long staffId);
    boolean existsByStaffId(Long staffId);

    /** [staffId, mpoIndexNumber] pairs for the public teacher list. */
    @Query("SELECT p.staff.id, p.mpoIndexNumber FROM TeacherProfile p WHERE p.staff.id IN :staffIds")
    List<Object[]> findMpoIndexByStaffIds(@Param("staffIds") Collection<Long> staffIds);
}
