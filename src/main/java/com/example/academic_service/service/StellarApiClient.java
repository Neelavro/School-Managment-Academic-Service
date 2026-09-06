package com.example.academic_service.service;

import com.example.academic_service.config.StellarProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "stellar.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class StellarApiClient {

    private final StellarProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public List<LogEntry> fetchLog(LocalDate startDate, LocalDate endDate,
                                   LocalTime startTime, LocalTime endTime,
                                   long accessId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("operation", "fetch_log");
        body.put("auth_user", props.getAuthUser());
        body.put("auth_code", props.getAuthCode());
        body.put("start_date", startDate.toString());
        body.put("end_date", endDate.toString());
        body.put("start_time", startTime.toString());
        body.put("end_time", endTime.toString());
        body.put("access_id", accessId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);

        try {
            String raw = restTemplate.postForObject(props.getBaseUrl(), req, String.class);
            if (raw == null || raw.isBlank()) return Collections.emptyList();
            LogResponse parsed = objectMapper.readValue(raw, LogResponse.class);
            return parsed.log != null ? parsed.log : Collections.emptyList();
        } catch (Exception e) {
            log.error("Stellar fetch_log failed (accessId={}): {}", accessId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LogResponse {
        public List<LogEntry> log;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LogEntry {
        public String unit_name;
        public String unit_id;
        public String registration_id;
        public String access_time;
        public String access_date;
        public String user_name;
        public String card;
        public String access_id;
    }
}
