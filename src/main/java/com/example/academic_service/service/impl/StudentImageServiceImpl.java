package com.example.academic_service.service.impl;

import com.example.academic_service.entity.Student;
import com.example.academic_service.entity.StudentImage;
import com.example.academic_service.repository.StudentImageRepository;
import com.example.academic_service.repository.StudentRepository;
import com.example.academic_service.service.StudentImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentImageServiceImpl implements StudentImageService {

    private final StudentImageRepository studentImageRepository;
    private final StudentRepository studentRepository;

    private static final String IMAGE_FOLDER = "/var/www/student-service-images/";
    private static final String BASE_URL      = "http://192.168.0.143:8084";
    private static final int    MAX_BYTES     = 20 * 1024; // 20 KB

    @Override
    public StudentImage addImage(Long studentId, MultipartFile file) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        byte[] imageBytes = compressImageToMaxSize(file);

        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename().replaceAll("\\s+", "_") : "photo.jpg";
        String fileName = UUID.randomUUID() + "_" + filename;
        Path filePath   = Paths.get(IMAGE_FOLDER + fileName);

        try {
            Files.write(filePath, imageBytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file", e);
        }

        StudentImage image = new StudentImage();
        image.setStudent(student);
        image.setImageUrl(BASE_URL + "/images/" + fileName);
        image.setIsActive(true);

        StudentImage saved = studentImageRepository.save(image);

        student.setImage(saved);
        studentRepository.save(student);

        return saved;
    }

    @Override
    public StudentImage addImageByStudentSystemId(Long studentId, MultipartFile file) {
        Student student = studentRepository.findByStudentSystemId(studentId.toString())
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        byte[] imageBytes = compressImageToMaxSize(file);

        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename().replaceAll("\\s+", "_") : "photo.jpg";
        String fileName = UUID.randomUUID() + "_" + filename;
        Path filePath   = Paths.get(IMAGE_FOLDER + fileName);

        try {
            Files.write(filePath, imageBytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file", e);
        }

        StudentImage image = new StudentImage();
        image.setStudent(student);
        image.setImageUrl(BASE_URL + "/images/" + fileName);
        image.setIsActive(true);

        StudentImage saved = studentImageRepository.save(image);

        student.setImage(saved);
        studentRepository.save(student);

        return saved;
    }

    @Override
    public Optional<StudentImage> getImageByStudent(Long studentId) {
        return studentImageRepository.findByStudentIdAndIsActiveTrue(studentId);
    }

    @Override
    public void deleteImage(Long imageId) {
        StudentImage image = studentImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + imageId));

        try {
            String fileName = image.getImageUrl()
                    .substring(image.getImageUrl().lastIndexOf("/") + 1);
            Path filePath = Paths.get(IMAGE_FOLDER + fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file", e);
        }

        studentImageRepository.delete(image);
    }

    @Override
    public void detachStudentFromImage(Long imageId) {
        StudentImage image = studentImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + imageId));
        image.setStudent(null);
        studentImageRepository.save(image);
    }

    // ── Compression ───────────────────────────────────────────────────────────

    private byte[] compressImageToMaxSize(MultipartFile file) {
        try {
            byte[] originalBytes = file.getBytes();
            return compress(originalBytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image bytes for compression", e);
        }
    }

    private byte[] compress(byte[] originalBytes) throws IOException {
        if (originalBytes.length <= MAX_BYTES) {
            log.info("Image already under 20KB ({} bytes) — no compression needed", originalBytes.length);
            return originalBytes;
        }

        BufferedImage bufferedImage;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(originalBytes)) {
            bufferedImage = ImageIO.read(bis);
        }

        if (bufferedImage == null) {
            log.warn("Could not decode image — saving original bytes");
            return originalBytes;
        }

        log.info("Compressing image (original: {} bytes) ...", originalBytes.length);

        float[] qualitySteps = { 0.85f, 0.75f, 0.60f, 0.45f, 0.30f, 0.20f, 0.10f, 0.05f };

        byte[] result = tryQualityStepping(bufferedImage, qualitySteps);
        if (result != null) return result;

        log.warn("Quality stepping insufficient — scaling to 50%");
        BufferedImage scaled50 = scaleImage(bufferedImage, 0.5);
        result = tryQualityStepping(scaled50, qualitySteps);
        if (result != null) return result;

        log.warn("50% scale insufficient — scaling to 25%");
        BufferedImage scaled25 = scaleImage(bufferedImage, 0.25);
        result = tryQualityStepping(scaled25, qualitySteps);
        if (result != null) return result;

        log.warn("Could not get image under 20KB — saving best effort result");
        return writeAtQuality(scaled25, 0.05f);
    }

    private byte[] tryQualityStepping(BufferedImage image, float[] steps) throws IOException {
        for (float quality : steps) {
            byte[] compressed = writeAtQuality(image, quality);
            log.info("  quality={} → {} bytes", quality, compressed.length);
            if (compressed.length <= MAX_BYTES) return compressed;
        }
        return null;
    }

    private byte[] writeAtQuality(BufferedImage image, float quality) throws IOException {
        ImageWriter     writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param  = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return bos.toByteArray();
    }

    private BufferedImage scaleImage(BufferedImage original, double scale) {
        int newWidth  = (int) (original.getWidth()  * scale);
        int newHeight = (int) (original.getHeight() * scale);

        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        scaled.getGraphics().drawImage(
                original.getScaledInstance(newWidth, newHeight, java.awt.Image.SCALE_SMOOTH),
                0, 0, null
        );
        return scaled;
    }
}
