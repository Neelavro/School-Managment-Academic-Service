package com.example.academic_service.service;

import com.example.academic_service.entity.AuditActionType;
import com.example.academic_service.entity.AuditLog;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // Full log with before/after snapshots (CREATE/UPDATE/DELETE)
    @Async
    public void log(Long userId, String ip, AuditActionType action,
                    Submodule submodule, String entityType, String entityId,
                    String description, String beforeValue, String afterValue) {
        save(userId, ip, action, submodule, entityType, entityId, description, beforeValue, afterValue);
    }

    // Lightweight log with no snapshots (mark entry, publish/unpublish)
    @Async
    public void log(Long userId, String ip, AuditActionType action,
                    Submodule submodule, String entityType, String entityId, String description) {
        save(userId, ip, action, submodule, entityType, entityId, description, null, null);
    }

    private void save(Long userId, String ip, AuditActionType action,
                      Submodule submodule, String entityType, String entityId,
                      String description, String beforeValue, String afterValue) {
        AuditLog entry = new AuditLog();
        entry.setUserId(userId);
        entry.setIpAddress(ip);
        entry.setActionType(action);
        entry.setSubmodule(submodule);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDescription(description);
        entry.setBeforeValue(beforeValue);
        entry.setAfterValue(afterValue);
        entry.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(entry);
    }

    public Page<AuditLog> search(Long userId, Submodule submodule, AuditActionType actionType,
                                  LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return auditLogRepository.search(userId, submodule, actionType, from, to, pageable);
    }
}
