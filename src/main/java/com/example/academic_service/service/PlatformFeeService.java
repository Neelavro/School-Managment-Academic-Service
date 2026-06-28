package com.example.academic_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fetches the PER_ENROLLMENT rate this school is billed at by the platform
 * owner (Edunix). Cached for an hour in Caffeine — see CacheConfig.platformFee.
 *
 * Configuration:
 *   platform.admin.url         — base URL of platform_admin_service (e.g. http://129.212.238.237:8085)
 *   platform.metrics.secret    — the same secret platform_admin uses to poll
 *                                us; sent here as X-Platform-Secret in the
 *                                opposite direction
 *
 * Failure modes are intentionally soft: if platform_admin is unreachable or
 * the secret is wrong, this returns {@link BigDecimal#ZERO} and logs a
 * warning. Missing platform fee is a degraded state, not a fatal one — the
 * student invoices still generate, just without the platform-fee line.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformFeeService {

    @Value("${platform.admin.url:}")
    private String platformAdminUrl;

    @Value("${platform.metrics.secret:}")
    private String secret;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Returns the PER_ENROLLMENT amount this school owes the platform per
     * active enrolment per month. Cached an hour by Caffeine; the cache key
     * is the constant string "current" because there's only one school per
     * academic_service instance.
     */
    @Cacheable(value = "platformFee", key = "'current'")
    public BigDecimal getPerEnrollmentRate() {
        if (platformAdminUrl == null || platformAdminUrl.isBlank()) {
            log.debug("Platform fee skipped — platform.admin.url not configured");
            return BigDecimal.ZERO;
        }
        if (secret == null || secret.isBlank()) {
            log.debug("Platform fee skipped — platform.metrics.secret not configured");
            return BigDecimal.ZERO;
        }
        String base = platformAdminUrl.endsWith("/") ? platformAdminUrl.substring(0, platformAdminUrl.length() - 1)
                                                     : platformAdminUrl;
        URI uri = URI.create(base + "/api/public/billing/rate");
        try {
            HttpRequest req = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(8))
                    .header("X-Platform-Secret", secret)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warn("Platform fee fetch HTTP {} — {}", res.statusCode(), res.body());
                return BigDecimal.ZERO;
            }
            JsonNode root = objectMapper.readTree(res.body());
            JsonNode data = root.path("data");
            JsonNode rateNode = data.path("perEnrollmentRate");
            if (rateNode.isMissingNode() || rateNode.isNull()) return BigDecimal.ZERO;
            return new BigDecimal(rateNode.asText("0"));
        } catch (Exception e) {
            log.warn("Platform fee fetch failed: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}
