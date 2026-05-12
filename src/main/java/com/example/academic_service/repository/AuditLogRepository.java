package com.example.academic_service.repository;

import com.example.academic_service.entity.AuditActionType;
import com.example.academic_service.entity.AuditLog;
import com.example.academic_service.entity.Submodule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:userId IS NULL OR a.userId = :userId) AND " +
            "(:submodule IS NULL OR a.submodule = :submodule) AND " +
            "(:actionType IS NULL OR a.actionType = :actionType) AND " +
            "(:from IS NULL OR a.timestamp >= :from) AND " +
            "(:to IS NULL OR a.timestamp <= :to) " +
            "ORDER BY a.timestamp DESC")
    Page<AuditLog> search(Long userId, Submodule submodule, AuditActionType actionType,
                          LocalDateTime from, LocalDateTime to, Pageable pageable);
}
