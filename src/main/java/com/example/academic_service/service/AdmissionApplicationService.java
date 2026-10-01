package com.example.academic_service.service;

import com.example.academic_service.entity.AcademicYear;
import com.example.academic_service.entity.AdmissionApplication;
import com.example.academic_service.entity.Class;
import com.example.academic_service.entity.Shift;
import com.example.academic_service.entity.StudentGroup;
import com.example.academic_service.entity.SystemSettings;
import com.example.academic_service.repository.AcademicYearRepository;
import com.example.academic_service.repository.AdmissionApplicationRepository;
import com.example.academic_service.repository.ClassRepository;
import com.example.academic_service.repository.ShiftRepository;
import com.example.academic_service.repository.StudentGroupRepository;
import com.example.academic_service.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Online admission: the public form's options and submission, and the admin
 * list / approve / reject. Approving only marks the application for now —
 * what it should create is decided separately.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdmissionApplicationService {

    // Same folder and URL scheme as student photos (served at /images/**).
    private static final String IMAGE_FOLDER = "/var/www/student-service-images/";
    private static final String BASE_URL = "http://167.172.86.59:8084";
    private static final int PHOTO_SIZE = 300;
    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;

    // The choices on the form (same as the institution's old EduZen form).
    public static final List<String> CATEGORIES = List.of("NON RESIDENT", "RESIDENTIAL", "DAY-CARE");
    private static final Set<String> GENDERS = Set.of("Male", "Female");
    private static final Set<String> RELIGIONS = Set.of("Islam", "Hindu", "Christian", "Buddist");
    private static final Set<String> NATIONALITIES = Set.of("Bangladeshi", "Others");
    private static final Set<String> GUARDIAN_TYPES = Set.of("Father", "Mother", "Other");

    private final AdmissionApplicationRepository repository;
    private final ClassRepository classRepository;
    private final ShiftRepository shiftRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SystemSettingsRepository systemSettingsRepository;

    // ── Public form ───────────────────────────────────────────────────────────

    /** Active classes (in panel order) with their shift and groups, plus the categories. */
    @Transactional(readOnly = true)
    public Map<String, Object> formOptions() {
        List<Map<String, Object>> classes = classRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .sorted(Comparator.comparing((Class c) -> c.getOrderIndex() == null ? Integer.MAX_VALUE : c.getOrderIndex())
                        .thenComparing(Class::getId))
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("name", c.getName());
                    Shift s = c.getShift();
                    m.put("shift", s == null ? null : Map.of("id", s.getId(), "name", s.getName()));
                    m.put("groups", activeGroups(c).stream()
                            .map(g -> Map.of("id", g.getId(), "name", g.getGroupName()))
                            .toList());
                    return m;
                })
                .toList();
        AcademicYear year = academicYearRepository.findFirstByIsActiveTrue().orElse(null);
        SystemSettings settings = systemSettingsRepository.findAll().stream().findFirst().orElse(null);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("institutionName", settings == null ? null : settings.getInstitutionName());
        out.put("address", settings == null ? null : settings.getAddress());
        out.put("hasLogo", settings != null && settings.getLogoUrl() != null && !settings.getLogoUrl().isBlank());
        out.put("academicYear", year == null ? null : year.getYearName());
        out.put("classes", classes);
        out.put("categories", CATEGORIES);
        return out;
    }

    /** The school logo's bytes for the form header, or null when there is none. */
    public byte[] logo() {
        String url = systemSettingsRepository.findAll().stream().findFirst()
                .map(SystemSettings::getLogoUrl).orElse(null);
        if (url == null || !url.contains("/images/")) return null;
        String name = url.substring(url.lastIndexOf("/images/") + "/images/".length());
        if (name.contains("/") || name.contains("..")) return null;
        try {
            return Files.readAllBytes(Paths.get(IMAGE_FOLDER + name));
        } catch (IOException e) {
            return null;
        }
    }

    /** Saves a submitted form; returns its application number. */
    @Transactional
    public Map<String, Object> submit(Map<String, Object> body, MultipartFile photo) {
        AdmissionApplication a = new AdmissionApplication();

        Class cls = classRepository.findByIdAndIsActiveTrue(requiredInt(body, "classId", "Class"))
                .orElseThrow(() -> bad("Class cannot be empty"));
        a.setClassId(cls.getId());
        Integer shiftId = requiredInt(body, "shiftId", "Shift");
        if (cls.getShift() == null || !cls.getShift().getId().equals(shiftId)) throw bad("Shift cannot be empty");
        a.setShiftId(shiftId);
        List<StudentGroup> groups = activeGroups(cls);
        Integer groupId = optionalInt(body, "groupId");
        if (groups.isEmpty()) {
            a.setStudentGroupId(null); // the form shows "N/A"
        } else {
            if (groupId == null || groups.stream().noneMatch(g -> g.getId().equals(groupId)))
                throw bad("Group cannot be empty");
            a.setStudentGroupId(groupId);
        }
        a.setCategory(oneOf(body, "category", "Category", Set.copyOf(CATEGORIES)));
        academicYearRepository.findFirstByIsActiveTrue().ifPresent(y -> a.setAcademicYearId(y.getId()));

        a.setApplicantName(required(body, "applicantName", "Applicant Name", 150));
        a.setGender(oneOf(body, "gender", "Gender", GENDERS));
        a.setReligion(oneOf(body, "religion", "Religion", RELIGIONS));
        a.setDob(date(body));
        a.setBloodGroup(optional(body, "bloodGroup", 5));
        a.setNationality(oneOf(body, "nationality", "Nationality", NATIONALITIES));
        a.setBirthCertificateNo(digits(optional(body, "birthCertificateNo", 17), "Birth Certificate/NID No"));
        a.setQuota(optional(body, "quota", 40));

        a.setFatherName(required(body, "fatherName", "Father's Name", 150));
        a.setFatherMobile(mobile(required(body, "fatherMobile", "Father's mobile", 11), "Father's mobile"));
        a.setFatherNid(required(body, "fatherNid", "Father's NID/Passport", 17));
        a.setFatherOccupation(optional(body, "fatherOccupation", 40));
        a.setFatherEducation(optional(body, "fatherEducation", 150));
        a.setFatherIncome(digits(optional(body, "fatherIncome", 20), "Father's Monthly Income"));

        a.setMotherName(required(body, "motherName", "Mother's Name", 150));
        a.setMotherMobile(mobile(required(body, "motherMobile", "Mother's mobile", 11), "Mother's mobile"));
        a.setMotherNid(required(body, "motherNid", "Mother's NID/passport", 17));
        a.setMotherOccupation(optional(body, "motherOccupation", 40));
        a.setMotherEducation(optional(body, "motherEducation", 150));
        a.setMotherIncome(digits(optional(body, "motherIncome", 20), "Mother's Monthly Income"));

        a.setPresentAddress(required(body, "presentAddress", "Present Address", 1000));
        a.setPermanentAddress(required(body, "permanentAddress", "Permanent Address", 1000));

        a.setGuardianType(oneOf(body, "guardianType", "Local guardian's Type", GUARDIAN_TYPES));
        a.setGuardianName(required(body, "guardianName", "Local guardian's name", 150));
        a.setGuardianMobile(mobile(required(body, "guardianMobile", "Local guardian's mobile no", 11), "Local guardian's mobile no"));
        a.setGuardianRelation(required(body, "guardianRelation", "Local guardian's Relation", 50));
        a.setGuardianOccupation(optional(body, "guardianOccupation", 100));

        a.setLastInstituteName(optional(body, "lastInstituteName", 200));
        a.setLastClassName(optional(body, "lastClassName", 100));
        a.setPreviousRoll(digits(optional(body, "previousRoll", 10), "Last Institute Roll"));
        a.setPreviousGpa(gpa(optional(body, "previousGpa", 4)));

        if (!Boolean.TRUE.equals(body.get("consent")))
            throw bad("Please confirm that all the information is true");

        a.setPhotoUrl(savePhoto(photo));
        AdmissionApplication saved = repository.save(a);
        saved.setApplicationNo(LocalDate.now(AdmissionApplication.DHAKA).getYear() + String.format("%05d", saved.getId()));
        repository.save(saved);
        log.info("Admission application {} received for class {}", saved.getApplicationNo(), cls.getName());
        return Map.of("applicationNo", saved.getApplicationNo());
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> list(String status, Integer classId, String q, int page, int size) {
        String st = status == null || status.isBlank() ? null : status.trim().toUpperCase();
        String query = q == null || q.isBlank() ? null : q.trim();
        Page<AdmissionApplication> result = repository.search(st, classId, query,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        Names names = names();
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put(AdmissionApplication.PENDING, 0L);
        counts.put(AdmissionApplication.APPROVED, 0L);
        counts.put(AdmissionApplication.REJECTED, 0L);
        for (Object[] row : repository.countByStatus()) counts.put((String) row[0], (Long) row[1]);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("content", result.getContent().stream().map(a -> summary(a, names)).toList());
        out.put("page", result.getNumber());
        out.put("size", result.getSize());
        out.put("totalElements", result.getTotalElements());
        out.put("totalPages", result.getTotalPages());
        out.put("counts", counts);
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id) {
        AdmissionApplication a = find(id);
        Names names = names();
        Map<String, Object> m = summary(a, names);
        m.put("religion", a.getReligion());
        m.put("dob", a.getDob());
        m.put("bloodGroup", a.getBloodGroup());
        m.put("nationality", a.getNationality());
        m.put("birthCertificateNo", a.getBirthCertificateNo());
        m.put("quota", a.getQuota());
        m.put("fatherNid", a.getFatherNid());
        m.put("fatherOccupation", a.getFatherOccupation());
        m.put("fatherEducation", a.getFatherEducation());
        m.put("fatherIncome", a.getFatherIncome());
        m.put("motherName", a.getMotherName());
        m.put("motherNid", a.getMotherNid());
        m.put("motherOccupation", a.getMotherOccupation());
        m.put("motherEducation", a.getMotherEducation());
        m.put("motherIncome", a.getMotherIncome());
        m.put("presentAddress", a.getPresentAddress());
        m.put("permanentAddress", a.getPermanentAddress());
        m.put("guardianType", a.getGuardianType());
        m.put("guardianName", a.getGuardianName());
        m.put("guardianRelation", a.getGuardianRelation());
        m.put("guardianMobile", a.getGuardianMobile());
        m.put("guardianOccupation", a.getGuardianOccupation());
        m.put("lastInstituteName", a.getLastInstituteName());
        m.put("lastClassName", a.getLastClassName());
        m.put("previousRoll", a.getPreviousRoll());
        m.put("previousGpa", a.getPreviousGpa());
        m.put("academicYear", a.getAcademicYearId() == null ? null
                : academicYearRepository.findById(a.getAcademicYearId()).map(AcademicYear::getYearName).orElse(null));
        return m;
    }

    @Transactional
    public Map<String, Object> approve(Long id, String reviewer) {
        return review(id, AdmissionApplication.APPROVED, null, reviewer);
    }

    @Transactional
    public Map<String, Object> reject(Long id, String note, String reviewer) {
        return review(id, AdmissionApplication.REJECTED, note, reviewer);
    }

    private Map<String, Object> review(Long id, String status, String note, String reviewer) {
        AdmissionApplication a = find(id);
        if (!AdmissionApplication.PENDING.equals(a.getStatus()))
            throw bad("This application was already " + a.getStatus().toLowerCase());
        a.setStatus(status);
        a.setReviewNote(note == null || note.isBlank() ? null : truncate(note.trim(), 500));
        a.setReviewedBy(reviewer);
        a.setReviewedAt(LocalDateTime.now(AdmissionApplication.DHAKA));
        repository.save(a);
        return get(id);
    }

    private AdmissionApplication find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private Map<String, Object> summary(AdmissionApplication a, Names names) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("applicationNo", a.getApplicationNo());
        m.put("status", a.getStatus());
        m.put("createdAt", a.getCreatedAt());
        m.put("classId", a.getClassId());
        m.put("className", names.classes.get(a.getClassId()));
        m.put("shiftName", a.getShiftId() == null ? null : names.shifts.get(a.getShiftId()));
        m.put("groupName", a.getStudentGroupId() == null ? null : names.groups.get(a.getStudentGroupId()));
        m.put("category", a.getCategory());
        m.put("applicantName", a.getApplicantName());
        m.put("gender", a.getGender());
        m.put("fatherName", a.getFatherName());
        m.put("fatherMobile", a.getFatherMobile());
        m.put("motherMobile", a.getMotherMobile());
        m.put("photoUrl", a.getPhotoUrl());
        m.put("reviewNote", a.getReviewNote());
        m.put("reviewedBy", a.getReviewedBy());
        m.put("reviewedAt", a.getReviewedAt());
        return m;
    }

    private record Names(Map<Integer, String> classes, Map<Integer, String> shifts, Map<Integer, String> groups) {}

    private Names names() {
        return new Names(
                classRepository.findAll().stream().collect(Collectors.toMap(Class::getId, Class::getName)),
                shiftRepository.findAll().stream().collect(Collectors.toMap(Shift::getId, Shift::getName)),
                studentGroupRepository.findAll().stream().collect(Collectors.toMap(StudentGroup::getId, StudentGroup::getGroupName)));
    }

    private static List<StudentGroup> activeGroups(Class c) {
        return c.getStudentGroups().stream()
                .filter(g -> !Boolean.FALSE.equals(g.getIsActive()))
                .sorted(Comparator.comparing(StudentGroup::getId))
                .toList();
    }

    // ── Photo ────────────────────────────────────────────────────────────────

    /** Required; JPEG/PNG up to 5 MB, stored as a JPEG that fits in 300 × 300. */
    private String savePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) throw bad("Student Photo cannot be empty");
        if (file.getSize() > MAX_PHOTO_BYTES) throw bad("Student Photo must be 5 MB or smaller");
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!type.equals("image/jpeg") && !type.equals("image/png")) throw bad("Please select image file");
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (src == null) throw bad("Please select image file");
            double scale = Math.min(1.0, (double) PHOTO_SIZE / Math.max(src.getWidth(), src.getHeight()));
            int w = Math.max(1, (int) Math.round(src.getWidth() * scale));
            int h = Math.max(1, (int) Math.round(src.getHeight() * scale));
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(java.awt.Color.WHITE); // PNG transparency → white
            g.fillRect(0, 0, w, h);
            g.drawImage(src, 0, 0, w, h, null);
            g.dispose();

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(bytes)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.85f);
                writer.write(null, new IIOImage(out, null, null), param);
            } finally {
                writer.dispose();
            }

            String fileName = "admission_" + UUID.randomUUID() + ".jpg";
            Path path = Paths.get(IMAGE_FOLDER + fileName);
            Files.write(path, bytes.toByteArray());
            return BASE_URL + "/images/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save the student photo", e);
        }
    }

    // ── Field checks (messages match the old form's) ──────────────────────────

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static String text(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static String required(Map<String, Object> body, String key, String label, int max) {
        String s = text(body, key);
        if (s == null) throw bad(label + " cannot be empty");
        if (s.length() > max) throw bad(label + " is too long");
        return s;
    }

    private static String optional(Map<String, Object> body, String key, int max) {
        String s = text(body, key);
        return s == null ? null : truncate(s, max);
    }

    private static String oneOf(Map<String, Object> body, String key, String label, Set<String> allowed) {
        String s = text(body, key);
        if (s == null || !allowed.contains(s)) throw bad(label + " cannot be empty");
        return s;
    }

    private static Integer requiredInt(Map<String, Object> body, String key, String label) {
        Integer v = optionalInt(body, key);
        if (v == null) throw bad(label + " cannot be empty");
        return v;
    }

    private static Integer optionalInt(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s && s.matches("\\d{1,9}")) return Integer.valueOf(s);
        return null;
    }

    private static String mobile(String s, String label) {
        if (!s.matches("01\\d{9}")) throw bad(label + " must be 11 digits starting with 01");
        return s;
    }

    private static String digits(String s, String label) {
        if (s != null && !s.matches("\\d+")) throw bad(label + " must contain digits only");
        return s;
    }

    private static String gpa(String s) {
        if (s == null) return null;
        if (!s.matches("\\d(\\.\\d{1,2})?")) throw bad("Enter GPA like 3.25");
        return s;
    }

    /** yyyy-MM-dd, between 1970 and today. */
    private static LocalDate date(Map<String, Object> body) {
        String s = text(body, "dob");
        if (s == null) throw bad("Date of birth cannot be empty");
        try {
            LocalDate d = LocalDate.parse(s);
            if (d.getYear() < 1970 || d.isAfter(LocalDate.now(AdmissionApplication.DHAKA)))
                throw bad("Please enter a valid date of birth");
            return d;
        } catch (DateTimeParseException e) {
            throw bad("Please enter a valid date of birth");
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
