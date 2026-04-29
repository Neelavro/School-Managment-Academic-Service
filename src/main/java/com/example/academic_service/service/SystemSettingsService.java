package com.example.academic_service.service;

import com.example.academic_service.entity.SystemSettings;
import org.springframework.web.multipart.MultipartFile;

public interface SystemSettingsService {
    SystemSettings getSettings();
    SystemSettings createSettings(String institutionName, String address, MultipartFile logo);
    SystemSettings updateSettings(String institutionName, String address);
    SystemSettings updateLogo(MultipartFile logo);
}
