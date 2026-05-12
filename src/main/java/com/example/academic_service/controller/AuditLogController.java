package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.AuditActionType;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.AuditLogService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @RequirePermission(submodule = Submodule.USER_MANAGEMENT, action = "READ")
    public ResponseEntity<ApiResponse> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Submodule submodule,
            @RequestParam(required = false) AuditActionType actionType,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        LocalDateTime fromDt = from != null ? LocalDateTime.parse(from) : null;
        LocalDateTime toDt   = to   != null ? LocalDateTime.parse(to)   : null;

        return ResponseEntity.ok(new ApiResponse("OK",
                auditLogService.search(userId, submodule, actionType, fromDt, toDt, PageRequest.of(page, size))));
    }
}
