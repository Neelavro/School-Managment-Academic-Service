package com.example.academic_service.client;

import com.example.academic_service.dto.StudentDto;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class StudentServiceClient {

    @Value("${student.service.url:http://167.172.86.59:8083}")
    private String studentServiceUrl;

    // Inject Spring's auto-configured ObjectMapper (already has JavaTimeModule registered)
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public Map<String, StudentDto> fetchAllStudentsAsMap() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(studentServiceUrl + "/api/students"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("student-service returned {}: {}", response.statusCode(), response.body());
                return Collections.emptyMap();
            }

            // Handle both plain list and paginated { content: [...] } response
            Object parsed = objectMapper.readValue(response.body(), Object.class);
            List<StudentDto> students;

            if (parsed instanceof List) {
                students = objectMapper.convertValue(parsed, new TypeReference<>() {});
            } else {
                Map<?, ?> map = (Map<?, ?>) parsed;
                Object content = map.get("content");
                if (content == null) {
                    log.error("Unexpected student-service response shape");
                    return Collections.emptyMap();
                }
                students = objectMapper.convertValue(content, new TypeReference<>() {});
            }

            return students.stream()
                    .collect(Collectors.toMap(
                            StudentDto::getStudentSystemId,
                            Function.identity(),
                            (a, b) -> a
                    ));

        } catch (Exception e) {
            log.error("Failed to fetch students from student-service", e);
            return Collections.emptyMap();
        }
    }
}