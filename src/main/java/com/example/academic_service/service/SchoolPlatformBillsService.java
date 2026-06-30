package com.example.academic_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Proxies the school-side "Platform Bills" view to the platform_admin
 * service. Same X-Platform-Secret pattern as {@link PlatformFeeService}.
 *
 * Why proxy rather than call platform_admin from the browser directly:
 *   1. The shared secret would have to live in the browser, defeating the
 *      whole point of having a secret.
 *   2. CORS — platform_admin is a separate origin.
 *   3. Auth — the school user is already authenticated to academic_service;
 *      the secret is a server-side concern.
 *
 * No caching here — billing history changes when admin records a payment,
 * and that's rare enough that fresh fetches are fine.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolPlatformBillsService {

    @Value("${platform.admin.url:}")
    private String platformAdminUrl;

    @Value("${platform.metrics.secret:}")
    private String secret;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public JsonNode fetchBills() {
        return callPlatform("/api/public/billing/bills", "GET", null);
    }

    /**
     * School-side "Mark as Paid" — forwards the payload to platform_admin's
     * /api/public/billing/bills/{id}/report-payment endpoint. The school is
     * identified by the X-Platform-Secret header at the upstream layer.
     */
    public JsonNode reportPayment(Long billId, String requestJson) {
        return callPlatform("/api/public/billing/bills/" + billId + "/report-payment",
                "POST", requestJson);
    }

    private JsonNode callPlatform(String path, String method, String body) {
        if (platformAdminUrl == null || platformAdminUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Platform admin URL not configured");
        }
        if (secret == null || secret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Platform metrics secret not configured");
        }
        String base = platformAdminUrl.endsWith("/")
                ? platformAdminUrl.substring(0, platformAdminUrl.length() - 1)
                : platformAdminUrl;
        URI uri = URI.create(base + path);
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("X-Platform-Secret", secret)
                    .header("Accept", "application/json");
            if ("POST".equalsIgnoreCase(method)) {
                reqBuilder
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            } else {
                reqBuilder.GET();
            }
            HttpResponse<String> res = http.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 401) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Platform admin rejected the secret");
            }
            if (res.statusCode() / 100 == 4) {
                String message = extractMessage(res.body(), res.statusCode());
                throw new ResponseStatusException(HttpStatus.valueOf(res.statusCode()), message);
            }
            if (res.statusCode() != 200) {
                log.warn("Platform call {} {} → HTTP {} — {}", method, path, res.statusCode(), res.body());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Platform admin returned " + res.statusCode());
            }
            JsonNode root = objectMapper.readTree(res.body());
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) {
                return objectMapper.createObjectNode();
            }
            return data;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception e) {
            log.warn("Platform call {} {} failed: {}", method, path, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not reach platform admin: " + e.getMessage());
        }
    }

    private String extractMessage(String body, int statusCode) {
        try {
            JsonNode node = objectMapper.readTree(body);
            String msg = node.path("message").asText(null);
            if (msg != null && !msg.isBlank()) return msg;
        } catch (Exception ignored) {}
        return "Platform admin returned " + statusCode;
    }
}
