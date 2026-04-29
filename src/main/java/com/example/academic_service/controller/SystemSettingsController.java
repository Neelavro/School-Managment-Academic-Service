package com.example.academic_service.controller;

import com.example.academic_service.entity.SystemSettings;
import com.example.academic_service.service.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/system-settings")
@RequiredArgsConstructor
public class SystemSettingsController {

    private final SystemSettingsService service;

    @GetMapping
    public ResponseEntity<SystemSettings> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SystemSettings> createSettings(
            @RequestParam String institutionName,
            @RequestParam(required = false) String address,
            @RequestParam(value = "file", required = false) MultipartFile logo) {
        return ResponseEntity.ok(service.createSettings(institutionName, address, logo));
    }

    @PatchMapping
    public ResponseEntity<SystemSettings> updateSettings(
            @RequestParam(required = false) String institutionName,
            @RequestParam(required = false) String address) {
        return ResponseEntity.ok(service.updateSettings(institutionName, address));
    }

    @PatchMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SystemSettings> updateLogo(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.updateLogo(file));
    }
}
