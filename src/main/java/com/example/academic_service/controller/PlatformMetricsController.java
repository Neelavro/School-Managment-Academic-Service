package com.example.academic_service.controller;

import com.example.academic_service.dto.platform.PlatformMetricsDto;
import com.example.academic_service.service.PlatformMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Returns the full metric snapshot to the platform_admin_service.
 *
 * Auth: shared-secret header X-Platform-Secret, value matches the
 * PLATFORM_METRICS_SECRET env var. JWT auth is bypassed for this endpoint via
 * SecurityConfig.permitAll, but the secret check below blocks unauthenticated
 * access.
 *
 * The endpoint is also excluded from the request-traffic counter so the
 * platform admin's polling doesn't skew the numbers.
 */
@RestController
@RequestMapping("/api/platform/metrics")
@RequiredArgsConstructor
public class PlatformMetricsController {

    private final PlatformMetricsService metricsService;

    @Value("${platform.metrics.secret:}")
    private String configuredSecret;

    @GetMapping
    public ResponseEntity<PlatformMetricsDto> snapshot(
            @RequestHeader(value = "X-Platform-Secret", required = false) String providedSecret) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Metrics endpoint not configured");
        }
        if (providedSecret == null || !constantTimeEquals(configuredSecret, providedSecret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad secret");
        }
        return ResponseEntity.ok(metricsService.snapshot());
    }

    /** Avoids leaking the secret length / timing. */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }
}
