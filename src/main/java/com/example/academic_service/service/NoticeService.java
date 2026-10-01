package com.example.academic_service.service;

import com.example.academic_service.entity.AdmissionApplication;
import com.example.academic_service.entity.Notice;
import com.example.academic_service.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Website notices: admin CRUD and the public board. Attachments (PDF/JPG/PNG) are stored in the
 * images folder under a random name and only ever served through the endpoints here.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeService {

    private static final String FOLDER = "/var/www/student-service-images/";
    private static final long MAX_ATTACHMENT_BYTES = 10L * 1024 * 1024;
    public static final Set<String> CATEGORIES = Set.of("general", "exam", "result", "admission", "holiday", "tender");
    private static final Map<String, String> TYPES = Map.of(
            "application/pdf", ".pdf",
            "image/jpeg", ".jpg",
            "image/png", ".png");

    private final NoticeRepository repository;

    public record Attachment(byte[] bytes, String name, String type) {}

    // ── Reads ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAll() {
        return repository.findAllByOrderByPinnedDescNoticeDateDescIdDesc().stream().map(n -> toMap(n, true)).toList();
    }

    /** The public board: published notices only, optionally the newest {@code limit}. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPublished(Integer limit) {
        List<Notice> all = repository.findAllByPublishedTrueOrderByPinnedDescNoticeDateDescIdDesc();
        if (limit != null && limit > 0 && all.size() > limit) all = all.subList(0, limit);
        return all.stream().map(n -> toMap(n, false)).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPublished(Long id) {
        return toMap(repository.findByIdAndPublishedTrue(id).orElseThrow(NoticeService::notFound), false);
    }

    @Transactional(readOnly = true)
    public Attachment attachment(Long id, boolean publishedOnly) {
        Notice n = (publishedOnly ? repository.findByIdAndPublishedTrue(id) : repository.findById(id))
                .orElseThrow(NoticeService::notFound);
        if (n.getAttachmentFile() == null) throw notFound();
        try {
            return new Attachment(Files.readAllBytes(Paths.get(FOLDER + n.getAttachmentFile())), n.getAttachmentName(), n.getAttachmentType());
        } catch (IOException e) {
            log.warn("Notice {} attachment missing: {}", id, n.getAttachmentFile());
            throw notFound();
        }
    }

    // ── Writes ─────────────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> create(Map<String, Object> body, MultipartFile file, String user) {
        Notice n = new Notice();
        apply(n, body);
        n.setCreatedBy(user);
        if (file != null && !file.isEmpty()) storeAttachment(n, file);
        return toMap(repository.save(n), true);
    }

    /** Body may carry {@code removeAttachment: true}; a new file replaces the old one. */
    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> body, MultipartFile file) {
        Notice n = repository.findById(id).orElseThrow(NoticeService::notFound);
        apply(n, body);
        String oldFile = n.getAttachmentFile();
        if (file != null && !file.isEmpty()) {
            storeAttachment(n, file);
        } else if (Boolean.TRUE.equals(body.get("removeAttachment"))) {
            n.setAttachmentFile(null);
            n.setAttachmentName(null);
            n.setAttachmentType(null);
            n.setAttachmentSize(null);
        }
        n.setUpdatedAt(LocalDateTime.now(AdmissionApplication.DHAKA));
        Notice saved = repository.save(n);
        if (oldFile != null && !oldFile.equals(saved.getAttachmentFile())) deleteFile(oldFile);
        return toMap(saved, true);
    }

    @Transactional
    public void delete(Long id) {
        Notice n = repository.findById(id).orElseThrow(NoticeService::notFound);
        repository.delete(n);
        if (n.getAttachmentFile() != null) deleteFile(n.getAttachmentFile());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void apply(Notice n, Map<String, Object> body) {
        n.setTitleBn(text(body, "titleBn", 300));
        n.setTitleEn(text(body, "titleEn", 300));
        if (n.getTitleBn() == null && n.getTitleEn() == null) throw bad("Write a title in Bangla or English");
        n.setBodyBn(text(body, "bodyBn", 20000));
        n.setBodyEn(text(body, "bodyEn", 20000));
        String date = text(body, "date", 10);
        if (date == null) throw bad("Pick the notice date");
        try {
            n.setNoticeDate(LocalDate.parse(date));
        } catch (DateTimeParseException e) {
            throw bad("Pick a valid notice date");
        }
        String category = text(body, "category", 20);
        if (category == null || !CATEGORIES.contains(category)) throw bad("Pick a category");
        n.setCategory(category);
        n.setPinned(Boolean.TRUE.equals(body.get("pinned")));
        n.setPublished(!Boolean.FALSE.equals(body.get("published")));
    }

    private void storeAttachment(Notice n, MultipartFile file) {
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        String ext = TYPES.get(type);
        if (ext == null) throw bad("The attachment must be a PDF, JPG or PNG file");
        if (file.getSize() > MAX_ATTACHMENT_BYTES) throw bad("The attachment must be 10 MB or smaller");
        String stored = "notice_" + UUID.randomUUID() + ext;
        try {
            Files.write(Paths.get(FOLDER + stored), file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save the attachment", e);
        }
        String original = file.getOriginalFilename() == null ? "attachment" + ext
                : Path.of(file.getOriginalFilename()).getFileName().toString();
        n.setAttachmentFile(stored);
        n.setAttachmentName(original.length() > 200 ? original.substring(original.length() - 200) : original);
        n.setAttachmentType(type);
        n.setAttachmentSize(file.getSize());
    }

    private static void deleteFile(String stored) {
        try {
            Files.deleteIfExists(Paths.get(FOLDER + stored));
        } catch (IOException e) {
            log.warn("Could not delete notice attachment {}", stored);
        }
    }

    private static Map<String, Object> toMap(Notice n, boolean admin) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("titleBn", n.getTitleBn());
        m.put("titleEn", n.getTitleEn());
        m.put("bodyBn", n.getBodyBn());
        m.put("bodyEn", n.getBodyEn());
        m.put("date", n.getNoticeDate());
        m.put("category", n.getCategory());
        m.put("pinned", n.getPinned());
        if (n.getAttachmentFile() != null) {
            m.put("attachment", Map.of(
                    "name", n.getAttachmentName() == null ? "attachment" : n.getAttachmentName(),
                    "type", n.getAttachmentType() == null ? "" : n.getAttachmentType(),
                    "sizeKb", n.getAttachmentSize() == null ? 0 : Math.max(1, Math.round(n.getAttachmentSize() / 1024.0))));
        } else {
            m.put("attachment", null);
        }
        if (admin) {
            m.put("published", n.getPublished());
            m.put("createdBy", n.getCreatedBy());
            m.put("createdAt", n.getCreatedAt());
            m.put("updatedAt", n.getUpdatedAt());
        }
        return m;
    }

    private static String text(Map<String, Object> body, String key, int max) {
        Object v = body.get(key);
        if (v == null) return null;
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        if (s.length() > max) throw bad("Text is too long");
        return s;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Notice not found");
    }
}
