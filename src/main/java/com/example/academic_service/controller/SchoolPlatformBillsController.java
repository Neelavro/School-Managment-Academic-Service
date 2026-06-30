package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.SchoolPlatformBillsService;
import com.example.academic_service.util.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Surface platform-billing history to the school's own admins.
 * Proxies {@link SchoolPlatformBillsService} which authenticates upstream
 * with the shared PLATFORM_METRICS_SECRET.
 */
@RestController
@RequestMapping("/api/platform-bills")
@RequiredArgsConstructor
public class SchoolPlatformBillsController {

    private final SchoolPlatformBillsService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    @RequirePermission(submodule = Submodule.ACCOUNTS_SETTINGS, action = "VIEW")
    public ResponseEntity<ApiResponse<JsonNode>> getMyBills() {
        return ResponseEntity.ok(new ApiResponse<>("Platform bills", service.fetchBills()));
    }

    /**
     * School "Mark as Paid" — forwards to platform_admin's
     * /api/public/billing/bills/{billId}/report-payment.
     *
     * Body: { "amount": <BigDecimal>, "note": "..." }
     */
    @PostMapping("/{billId}/report-payment")
    @RequirePermission(submodule = Submodule.ACCOUNTS_SETTINGS, action = "UPDATE")
    public ResponseEntity<ApiResponse<JsonNode>> reportPayment(
            @PathVariable Long billId,
            @RequestBody Map<String, Object> body) {
        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request body");
        }
        return ResponseEntity.ok(new ApiResponse<>("Payment reported",
                service.reportPayment(billId, json)));
    }
}
