package com.example.academic_service.service.impl;

import com.example.academic_service.entity.SystemSettings;
import com.example.academic_service.repository.SystemSettingsRepository;
import com.example.academic_service.service.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingsServiceImpl implements SystemSettingsService {

    private final SystemSettingsRepository repository;

    private static final String IMAGE_FOLDER = "/var/www/student-service-images/";
    private static final String BASE_URL = "http://192.168.0.143:8084";
    @Override
    public SystemSettings getSettings() {
        return repository.findAll().stream().findFirst()
                .orElseGet(() -> repository.save(new SystemSettings()));
    }

    @Override
    public SystemSettings createSettings(String institutionName, String address, MultipartFile logo) {
        if (repository.count() > 0) throw new RuntimeException("Settings already exist. Use PATCH to update.");

        SystemSettings settings = new SystemSettings();
        settings.setInstitutionName(institutionName);
        settings.setAddress(address);
        settings = repository.save(settings);

        if (logo != null && !logo.isEmpty()) {
            try {
                String filename = "logo_" + UUID.randomUUID() + "." + getExtension(logo.getOriginalFilename());
                Path path = Paths.get(IMAGE_FOLDER + filename);
                Files.createDirectories(path.getParent());
                Files.write(path, logo.getBytes());
                settings.setLogoUrl(BASE_URL + "/images/" + filename);
                settings = repository.save(settings);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save logo: " + e.getMessage());
            }
        }

        return settings;
    }

    @Override
    public SystemSettings updateSettings(String institutionName, String address) {
        SystemSettings settings = getSettings();
        if (institutionName != null) settings.setInstitutionName(institutionName);
        if (address != null) settings.setAddress(address);
        return repository.save(settings);
    }

    @Override
    public SystemSettings updateLogo(MultipartFile logo) {
        SystemSettings settings = getSettings();

        try {
            String filename = "logo_" + UUID.randomUUID() + "." + getExtension(logo.getOriginalFilename());
            Path path = Paths.get(IMAGE_FOLDER + filename);
            Files.createDirectories(path.getParent());
            Files.write(path, logo.getBytes());

            // delete old logo file if exists
            if (settings.getLogoUrl() != null) {
                String oldFilename = settings.getLogoUrl().substring(settings.getLogoUrl().lastIndexOf('/') + 1);
                Path oldPath = Paths.get(IMAGE_FOLDER + oldFilename);
                Files.deleteIfExists(oldPath);
            }

            settings.setLogoUrl(BASE_URL + "/images/" + filename);
            return repository.save(settings);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save logo: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename != null && filename.contains(".")) return filename.substring(filename.lastIndexOf('.') + 1);
        return "jpg";
    }
}
