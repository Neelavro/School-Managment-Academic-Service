package com.example.academic_service.repository;

import com.example.academic_service.entity.StaffDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffDocumentRepository extends JpaRepository<StaffDocument, Long> {
    List<StaffDocument> findByStaffId(Long staffId);
}
