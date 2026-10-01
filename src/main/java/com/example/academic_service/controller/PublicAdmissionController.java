package com.example.academic_service.controller;

import com.example.academic_service.service.AdmissionApplicationService;
import com.example.academic_service.util.ApiResponse;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * The public online-admission form (no login; permitted in SecurityConfig).
 * The website posts here through its own server, so no CORS is needed.
 */
@RestController
@RequestMapping("/api/public/admission")
@RequiredArgsConstructor
public class PublicAdmissionController {

    private final AdmissionApplicationService service;
    private final JsonMapper objectMapper;

    @GetMapping("/options")
    public ResponseEntity<ApiResponse> options() {
        return ResponseEntity.ok(new ApiResponse("OK", service.formOptions()));
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo() {
        byte[] bytes = service.logo();
        if (bytes == null) return ResponseEntity.notFound().build();
        String type = bytes.length > 3 && (bytes[0] & 0xff) == 0x89 ? MediaType.IMAGE_PNG_VALUE : MediaType.IMAGE_JPEG_VALUE;
        return ResponseEntity.ok().header("Content-Type", type).header("Cache-Control", "public, max-age=3600").body(bytes);
    }

    /** Multipart: {@code data} = the form as JSON, {@code photo} = the student photo. */
    @PostMapping(value = "/applications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> submit(@RequestPart("data") String data,
                                              @RequestPart(value = "photo", required = false) MultipartFile photo) {
        Map<String, Object> body;
        try {
            body = objectMapper.readValue(data, new TypeReference<>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid form data");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Your application has been submitted", service.submit(body, photo)));
    }
}
