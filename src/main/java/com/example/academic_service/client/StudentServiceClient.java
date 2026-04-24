package com.example.academic_service.client;

import com.example.academic_service.dto.EnrollmentWithStudentRequestDto;
import com.example.academic_service.dto.StudentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class StudentServiceClient {

//    @Value("${student.service.url:http://192.168.0.210:8083}")
    @Value("${student.service.url:http://167.172.86.59:8083}")
    private String studentServiceUrl;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ── Find single student by systemId ──────────────────────────────────────
    public StudentDto findBySystemId(String studentSystemId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(studentServiceUrl + "/api/students/system/" + studentSystemId))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                log.info("Student not found for systemId: {}", studentSystemId);
                return null;
            }
            if (response.statusCode() != 200) {
                log.error("student-service returned {} for systemId {}: {}",
                        response.statusCode(), studentSystemId, response.body());
                return null;
            }

            return objectMapper.readValue(response.body(), StudentDto.class);

        } catch (Exception e) {
            log.error("Failed to fetch student by systemId {}: {}", studentSystemId, e.getMessage());
            return null;
        }
    }

    // ── Find single student by DB id ─────────────────────────────────────────
    public StudentDto fetchStudentById(Long id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(studentServiceUrl + "/api/students/" + id))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                log.info("Student not found for DB id: {}", id);
                return null;
            }
            if (response.statusCode() != 200) {
                log.error("student-service returned {} for DB id {}: {}",
                        response.statusCode(), id, response.body());
                return null;
            }

            return objectMapper.readValue(response.body(), StudentDto.class);

        } catch (Exception e) {
            log.error("Failed to fetch student by DB id {}: {}", id, e.getMessage());
            return null;
        }
    }

    // ── Create student -> returns DB id ───────────────────────────────────────
    public Long createStudent(EnrollmentWithStudentRequestDto request) {
        try {
            Map<String, Object> body = new HashMap<>();
            if (request.getStudentSystemId() != null)
                body.put("studentSystemId",        request.getStudentSystemId());
            body.put("nameEnglish",                request.getNameEnglish());
            body.put("nameBangla",                 request.getNameBangla());
            body.put("fatherNameEnglish",          request.getFatherNameEnglish());
            body.put("fatherNameBangla",           request.getFatherNameBangla());
            body.put("fatherOccupation",           request.getFatherOccupation());
            body.put("fatherPhone",                request.getFatherPhone());
            body.put("fatherMonthlySalary",        request.getFatherMonthlySalary());
            body.put("motherNameEnglish",          request.getMotherNameEnglish());
            body.put("motherNameBangla",           request.getMotherNameBangla());
            body.put("motherOccupation",           request.getMotherOccupation());
            body.put("motherPhone",                request.getMotherPhone());
            body.put("motherMonthlySalary",        request.getMotherMonthlySalary());
            body.put("guardianNameEnglish",        request.getGuardianNameEnglish());
            body.put("guardianNameBangla",         request.getGuardianNameBangla());
            body.put("guardianOccupation",         request.getGuardianOccupation());
            body.put("guardianPhone",              request.getGuardianPhone());
            body.put("guardianRelation",           request.getGuardianRelation());
            body.put("currentHoldingNo",           request.getCurrentHoldingNo());
            body.put("currentRoadOrVillage",       request.getCurrentRoadOrVillage());
            body.put("currentDistrict",            request.getCurrentDistrict());
            body.put("currentThana",               request.getCurrentThana());
            body.put("permanentHoldingNo",         request.getPermanentHoldingNo());
            body.put("permanentRoadOrVillage",     request.getPermanentRoadOrVillage());
            body.put("permanentDistrict",          request.getPermanentDistrict());
            body.put("permanentThana",             request.getPermanentThana());
            body.put("dob",                        request.getDob());
            body.put("nationality",                request.getNationality());
            if (request.getGenderId() != null)
                body.put("gender",        Map.of("id", request.getGenderId()));
            if (request.getStudentStatusId() != null)
                body.put("studentStatus", Map.of("id", request.getStudentStatusId()));

            String json = objectMapper.writeValueAsString(body);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(studentServiceUrl + "/api/students"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                log.error("student-service create returned {}: {}",
                        response.statusCode(), response.body());
                throw new RuntimeException("Could not create student in student-service: "
                        + response.body());
            }

            Map<?, ?> result = objectMapper.readValue(response.body(), Map.class);
            Object idVal = result.get("id");
            if (idVal == null) {
                throw new RuntimeException("student-service did not return an id after create");
            }
            return ((Number) idVal).longValue();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create student in student-service: {}", e.getMessage());
            throw new RuntimeException("Could not create student in student-service", e);
        }
    }

    public void updateStudent(String studentSystemId, EnrollmentWithStudentRequestDto request) {
        try {
            // First get the student's DB id
            StudentDto existing = findBySystemId(studentSystemId);
            if (existing == null) {
                throw new RuntimeException("Student not found for systemId: " + studentSystemId);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("studentSystemId",        studentSystemId);
            if (request.getNameEnglish()            != null) body.put("nameEnglish",            request.getNameEnglish());
            if (request.getNameBangla()             != null) body.put("nameBangla",             request.getNameBangla());
            if (request.getFatherNameEnglish()      != null) body.put("fatherNameEnglish",      request.getFatherNameEnglish());
            if (request.getFatherNameBangla()       != null) body.put("fatherNameBangla",       request.getFatherNameBangla());
            if (request.getFatherOccupation()       != null) body.put("fatherOccupation",       request.getFatherOccupation());
            if (request.getFatherPhone()            != null) body.put("fatherPhone",            request.getFatherPhone());
            if (request.getFatherMonthlySalary()    != null) body.put("fatherMonthlySalary",    request.getFatherMonthlySalary());
            if (request.getMotherNameEnglish()      != null) body.put("motherNameEnglish",      request.getMotherNameEnglish());
            if (request.getMotherNameBangla()       != null) body.put("motherNameBangla",       request.getMotherNameBangla());
            if (request.getMotherOccupation()       != null) body.put("motherOccupation",       request.getMotherOccupation());
            if (request.getMotherPhone()            != null) body.put("motherPhone",            request.getMotherPhone());
            if (request.getMotherMonthlySalary()    != null) body.put("motherMonthlySalary",    request.getMotherMonthlySalary());
            if (request.getGuardianNameEnglish()    != null) body.put("guardianNameEnglish",    request.getGuardianNameEnglish());
            if (request.getGuardianNameBangla()     != null) body.put("guardianNameBangla",     request.getGuardianNameBangla());
            if (request.getGuardianOccupation()     != null) body.put("guardianOccupation",     request.getGuardianOccupation());
            if (request.getGuardianPhone()          != null) body.put("guardianPhone",          request.getGuardianPhone());
            if (request.getGuardianRelation()       != null) body.put("guardianRelation",       request.getGuardianRelation());
            if (request.getCurrentHoldingNo()       != null) body.put("currentHoldingNo",       request.getCurrentHoldingNo());
            if (request.getCurrentRoadOrVillage()   != null) body.put("currentRoadOrVillage",   request.getCurrentRoadOrVillage());
            if (request.getCurrentDistrict()        != null) body.put("currentDistrict",        request.getCurrentDistrict());
            if (request.getCurrentThana()           != null) body.put("currentThana",           request.getCurrentThana());
            if (request.getPermanentHoldingNo()     != null) body.put("permanentHoldingNo",     request.getPermanentHoldingNo());
            if (request.getPermanentRoadOrVillage() != null) body.put("permanentRoadOrVillage", request.getPermanentRoadOrVillage());
            if (request.getPermanentDistrict()      != null) body.put("permanentDistrict",      request.getPermanentDistrict());
            if (request.getPermanentThana()         != null) body.put("permanentThana",         request.getPermanentThana());
            if (request.getDob()                    != null) body.put("dob",                    request.getDob());
            if (request.getNationality()            != null) body.put("nationality",            request.getNationality());
            if (request.getGenderId()               != null) body.put("gender",                 Map.of("id", request.getGenderId()));
            if (request.getStudentStatusId()        != null) body.put("studentStatus",          Map.of("id", request.getStudentStatusId()));

            String json = objectMapper.writeValueAsString(body);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(studentServiceUrl + "/api/students/" + existing.getId()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("student-service update returned {}: {}",
                        response.statusCode(), response.body());
                throw new RuntimeException("Could not update student: " + response.body());
            }

            log.info("Student updated successfully: {}", studentSystemId);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update student {}: {}", studentSystemId, e.getMessage());
            throw new RuntimeException("Could not update student in student-service", e);
        }
    }
    // ── Upload image using student DB id ──────────────────────────────────────
    public void uploadStudentImage(Long studentDbId, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            log.info("No image provided for student DB id: {}", studentDbId);
            return;
        }

        try {
            byte[] imageBytes = image.getBytes();
            String filename   = image.getOriginalFilename() != null
                    ? image.getOriginalFilename() : "photo.jpg";

            String boundary      = "----Boundary" + UUID.randomUUID().toString().replace("-", "");
            byte[] multipartBody = buildMultipartBody(boundary, filename, imageBytes);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(studentServiceUrl + "/api/students/images/" + studentDbId))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                log.error("Image upload failed for student {}: {} -- {}",
                        studentDbId, response.statusCode(), response.body());
                throw new RuntimeException("Image upload failed: " + response.body());
            }

            log.info("Image uploaded successfully for student DB id: {}", studentDbId);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload image for student {}: {}", studentDbId, e.getMessage());
            throw new RuntimeException("Could not upload student image", e);
        }
    }

    // ── Multipart body builder ────────────────────────────────────────────────
    private byte[] buildMultipartBody(String boundary, String filename, byte[] fileBytes)
            throws IOException {
        String CRLF = "\r\n";
        String partHeader =
                "--" + boundary + CRLF +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"" + CRLF +
                        "Content-Type: image/jpeg" + CRLF +
                        CRLF;
        String closingBoundary = CRLF + "--" + boundary + "--" + CRLF;

        byte[] headerBytes  = partHeader.getBytes(StandardCharsets.UTF_8);
        byte[] closingBytes = closingBoundary.getBytes(StandardCharsets.UTF_8);

        byte[] result = new byte[headerBytes.length + fileBytes.length + closingBytes.length];
        System.arraycopy(headerBytes,  0, result, 0,                                     headerBytes.length);
        System.arraycopy(fileBytes,    0, result, headerBytes.length,                    fileBytes.length);
        System.arraycopy(closingBytes, 0, result, headerBytes.length + fileBytes.length, closingBytes.length);

        return result;
    }

    // ── Fetch all students as map (keyed by studentSystemId) ─────────────────
    public Map<String, StudentDto> fetchAllStudentsAsMap() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(studentServiceUrl + "/api/students"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("student-service returned {}: {}",
                        response.statusCode(), response.body());
                return Collections.emptyMap();
            }

            Object parsed = objectMapper.readValue(response.body(), Object.class);
            List<StudentDto> students;

            if (parsed instanceof List) {
                students = objectMapper.convertValue(parsed, new TypeReference<>() {});
            } else {
                Map<?, ?> map = (Map<?, ?>) parsed;
                Object list = map.get("data");
                if (list == null) list = map.get("content");
                if (list == null) {
                    log.error("Unexpected student-service response shape: {}", map.keySet());
                    return Collections.emptyMap();
                }
                students = objectMapper.convertValue(list, new TypeReference<>() {});
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