package com.example.academic_service.repository;

import com.example.academic_service.entity.AdmissionApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionApplicationRepository extends JpaRepository<AdmissionApplication, Long> {

    /** Newest first; every filter is optional. {@code q} matches the application no, applicant name or a guardian's mobile. */
    @Query("""
            SELECT a FROM AdmissionApplication a
            WHERE (:status IS NULL OR a.status = :status)
              AND (:classId IS NULL OR a.classId = :classId)
              AND (:q IS NULL OR a.applicationNo LIKE CONCAT('%', :q, '%')
                   OR LOWER(a.applicantName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR a.fatherMobile LIKE CONCAT('%', :q, '%')
                   OR a.motherMobile LIKE CONCAT('%', :q, '%')
                   OR a.guardianMobile LIKE CONCAT('%', :q, '%'))
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    Page<AdmissionApplication> search(String status, Integer classId, String q, Pageable pageable);

    @Query("SELECT a.status, COUNT(a) FROM AdmissionApplication a GROUP BY a.status")
    List<Object[]> countByStatus();
}
