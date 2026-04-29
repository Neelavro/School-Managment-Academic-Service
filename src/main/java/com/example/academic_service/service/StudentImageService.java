package com.example.academic_service.service;

import com.example.academic_service.entity.StudentImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface StudentImageService {
    StudentImage addImage(Long studentId, MultipartFile file);
    StudentImage addImageByStudentSystemId(Long studentId, MultipartFile file);
    Optional<StudentImage> getImageByStudent(Long studentId);
    void deleteImage(Long imageId);
    void detachStudentFromImage(Long imageId);
}
