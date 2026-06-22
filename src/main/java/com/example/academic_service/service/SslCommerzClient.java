package com.example.academic_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Thin HTTP wrapper around the SSLCommerz REST endpoints.
 * Sandbox / production base URLs flip on the {@code sslcommerz.sandbox} property.
 *
 * Configuration (via .env):
 *   SSLCOMMERZ_STORE_ID       — store id (sandbox default: "testbox")
 *   SSLCOMMERZ_STORE_PASSWORD — store password (sandbox default: "qwerty")
 *   SSLCOMMERZ_SANDBOX        — true (default) for sandbox endpoints
 */
@Component
public class SslCommerzClient {

    private static final String SANDBOX_INIT = "https://sandbox.sslcommerz.com/gwprocess/v4/api.php";
    private static final String SANDBOX_VAL  = "https://sandbox.sslcommerz.com/validator/api/validationserverAPI.php";
    private static final String LIVE_INIT    = "https://securepay.sslcommerz.com/gwprocess/v4/api.php";
    private static final String LIVE_VAL     = "https://securepay.sslcommerz.com/validator/api/validationserverAPI.php";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ObjectMapper json = new ObjectMapper();

    @Value("${sslcommerz.store-id:testbox}")
    private String storeId;

    @Value("${sslcommerz.store-password:qwerty}")
    private String storePassword;

    @Value("${sslcommerz.sandbox:true}")
    private boolean sandbox;

    public String getStoreId() { return storeId; }
    public boolean isSandbox() { return sandbox; }

    private String initEndpoint() { return sandbox ? SANDBOX_INIT : LIVE_INIT; }
    private String validateEndpoint() { return sandbox ? SANDBOX_VAL : LIVE_VAL; }

    /**
     * Calls SSLCommerz Init API to create a payment session.
     * Returns the JSON response — the caller pulls GatewayPageURL out.
     *
     * @param fields form fields to send (must already include store_id/store_passwd)
     */
    public JsonNode createSession(Map<String, String> fields) {
        try {
            String body = toFormUrlEncoded(fields);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(initEndpoint()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "SSLCommerz init returned HTTP " + res.statusCode());
            }
            JsonNode root = json.readTree(res.body());
            String status = root.path("status").asText("");
            if (!"SUCCESS".equalsIgnoreCase(status)) {
                String reason = root.path("failedreason").asText("Unknown reason");
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "SSLCommerz init failed: " + status + " — " + reason);
            }
            return root;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "SSLCommerz init request failed: " + e.getMessage());
        }
    }

    /**
     * Calls the Validation API to verify a transaction by val_id.
     * Returns the JSON response — the caller checks status (VALID / VALIDATED / etc.).
     */
    public JsonNode validate(String valId) {
        try {
            String url = validateEndpoint()
                    + "?val_id=" + URLEncoder.encode(valId, StandardCharsets.UTF_8)
                    + "&store_id=" + URLEncoder.encode(storeId, StandardCharsets.UTF_8)
                    + "&store_passwd=" + URLEncoder.encode(storePassword, StandardCharsets.UTF_8)
                    + "&v=1&format=json";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "SSLCommerz validate returned HTTP " + res.statusCode());
            }
            return json.readTree(res.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "SSLCommerz validate request failed: " + e.getMessage());
        }
    }

    private static String toFormUrlEncoded(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : fields.entrySet()) {
            if (e.getValue() == null) continue;
            if (!first) sb.append('&');
            first = false;
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    public String getStorePassword() { return storePassword; }
}
