package com.example.academic_service.service;

import com.example.academic_service.entity.AuditActionType;
import com.example.academic_service.entity.AuditLog;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(Long userId, String ipAddress, AuditActionType actionType,
                    Submodule submodule, String entityType, String entityId, String description) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setIpAddress(ipAddress);
        log.setActionType(actionType);
        log.setSubmodule(submodule);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDescription(description);
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    public Page<AuditLog> search(Long userId, Submodule submodule, AuditActionType actionType,
                                  LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return auditLogRepository.search(userId, submodule, actionType, from, to, pageable);
    }
}
