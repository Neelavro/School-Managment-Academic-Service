package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.NoticeService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Website notices. Admin side under /api/notices (System Settings permission); the public board
 * under /api/public/notices (permitAll in SecurityConfig, published notices only).
 */
@RestController
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService service;
    private final JsonMapper jsonMapper;

    // ── Admin ──────────────────────────────────────────────────────────────────

    @GetMapping("/api/notices")
    @RequirePermission(submodule = Submodule.SYSTEM_SETTINGS, action = "READ")
    public ResponseEntity<ApiResponse> list() {
        return ResponseEntity.ok(new ApiResponse("OK", service.listAll()));
    }

    /** Multipart: {@code data} = JSON {titleBn, titleEn, bodyBn, bodyEn, date, category, pinned, published}, {@code file} = optional attachment. */
    @PostMapping(value = "/api/notices", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(submodule = Submodule.SYSTEM_SETTINGS, action = "CREATE")
    public ResponseEntity<ApiResponse> create(@RequestPart("data") String data,
                                              @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Notice created", service.create(parse(data), file, currentUser())));
    }

    /** Same parts as create; {@code data.removeAttachment: true} drops the current attachment. */
    @PutMapping(value = "/api/notices/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(submodule = Submodule.SYSTEM_SETTINGS, action = "UPDATE")
    public ResponseEntity<ApiResponse> update(@PathVariable Long id,
                                              @RequestPart("data") String data,
                                              @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(new ApiResponse("Notice updated", service.update(id, parse(data), file)));
    }

    @DeleteMapping("/api/notices/{id}")
    @RequirePermission(submodule = Submodule.SYSTEM_SETTINGS, action = "DELETE")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiResponse("Notice deleted", null));
    }

    @GetMapping("/api/notices/{id}/attachment")
    @RequirePermission(submodule = Submodule.SYSTEM_SETTINGS, action = "READ")
    public ResponseEntity<byte[]> adminAttachment(@PathVariable Long id) {
        return file(service.attachment(id, false));
    }

    // ── Public ─────────────────────────────────────────────────────────────────

    @GetMapping("/api/public/notices")
    public ResponseEntity<ApiResponse> published(@RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(new ApiResponse("OK", service.listPublished(limit)));
    }

    @GetMapping("/api/public/notices/{id}")
    public ResponseEntity<ApiResponse> publishedOne(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse("OK", service.getPublished(id)));
    }

    @GetMapping("/api/public/notices/{id}/attachment")
    public ResponseEntity<byte[]> publicAttachment(@PathVariable Long id) {
        return file(service.attachment(id, true));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> file(NoticeService.Attachment a) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, a.type() == null || a.type().isBlank() ? MediaType.APPLICATION_OCTET_STREAM_VALUE : a.type())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(a.name(), StandardCharsets.UTF_8).build().toString())
                .body(a.bytes());
    }

    private Map<String, Object> parse(String data) {
        try {
            return jsonMapper.readValue(data, new TypeReference<>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid notice data");
        }
    }

    private static String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }
}
