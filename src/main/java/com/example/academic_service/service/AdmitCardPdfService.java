package com.example.academic_service.service;

import com.example.academic_service.dto.EnrollmentResponseDto;
import com.example.academic_service.dto.admit_card_dtos.AdmitCardAllocationDto;
import com.example.academic_service.dto.admit_card_dtos.AdmitCardRoutineResponseDto;
import com.example.academic_service.dto.admit_card_dtos.AdmitCardSessionDto;
import com.example.academic_service.dto.admit_card_dtos.AdmitCardStudentDto;
import com.example.academic_service.entity.Enrollment;
import com.example.academic_service.repository.EnrollmentRepository;
import com.example.academic_service.repository.EnrollmentSpecification;
import com.example.academic_service.service.impl.AdmitCardServiceImpl;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Margin;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdmitCardPdfService {

    private static final int PAGE_W = 794;
    private static final int PAGE_H = 1123;

    private final AdmitCardServiceImpl admitCardService;
    private final EnrollmentRepository enrollmentRepository;
    private final IdCardPdfService idCardPdfService;

    private String logoBase64;
    private String signatureBase64;

    @PostConstruct
    public void init() {
        try {
            InputStream is = getClass().getResourceAsStream("/static/logo.png");
            logoBase64 = "data:image/png;base64,"
                    + Base64.getEncoder().encodeToString(is.readAllBytes());
        } catch (Exception e) {
            logoBase64 = "";
            System.err.println("Warning: Could not load logo. " + e.getMessage());
        }
        try {
            InputStream is = getClass().getResourceAsStream("/static/signature.png");
            signatureBase64 = "data:image/jpeg;base64,"
                    + Base64.getEncoder().encodeToString(is.readAllBytes());
        } catch (Exception e) {
            signatureBase64 = "";
            System.err.println("Warning: Could not load signature. " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FLOW 1 — Room-based
    // ═══════════════════════════════════════════════════════════════════════

    public byte[] generateAdmitCards(
            Integer routineId,
            Integer sessionId,
            Integer classId,
            Integer genderSectionId,
            Long sectionId
    ) throws Exception {
        AdmitCardRoutineResponseDto routine = admitCardService.getAdmitCardData(
                routineId, sessionId, classId, genderSectionId, sectionId);

        Set<String> allSystemIds = new LinkedHashSet<>();
        for (AdmitCardSessionDto session : routine.getSessions()) {
            for (AdmitCardAllocationDto alloc : session.getAllocations()) {
                for (AdmitCardStudentDto student : alloc.getStudents()) {
                    if (student.getStudentSystemId() != null)
                        allSystemIds.add(student.getStudentSystemId());
                }
            }
        }

        List<EnrollmentResponseDto> allEnrollments = fetchEnrollments(
                null, classId, sectionId, null, genderSectionId, null, null, null);

        Map<String, EnrollmentResponseDto> enrollmentMap = allEnrollments.stream()
                .filter(e -> e.getStudentSystemId() != null)
                .collect(Collectors.toMap(
                        EnrollmentResponseDto::getStudentSystemId,
                        e -> e,
                        (a, b) -> a));

        Map<String, StudentAdmitData> studentDataMap = new LinkedHashMap<>();
        for (AdmitCardSessionDto session : routine.getSessions()) {
            for (AdmitCardAllocationDto alloc : session.getAllocations()) {
                for (AdmitCardStudentDto student : alloc.getStudents()) {
                    String sid = student.getStudentSystemId();
                    if (sid == null) continue;
                    studentDataMap.computeIfAbsent(sid, k -> new StudentAdmitData(
                            sid,
                            student.getClassRoll(),
                            alloc.getRoomName(),
                            alloc.getSectionName(),
                            alloc.getGenderSectionName(),
                            session.getClassName()
                    )).addSession(session);
                }
            }
        }

        for (StudentAdmitData data : studentDataMap.values()) {
            if (!data.sessions.isEmpty()) {
                List<AdmitCardSessionDto> fullSchedule = data.sessions.get(0).getFullSchedule();
                if (fullSchedule != null && !fullSchedule.isEmpty()) {
                    data.sessions = new ArrayList<>(fullSchedule);
                    data.sessions.sort(Comparator.comparing(AdmitCardSessionDto::getDate));
                }
            }
        }

        String html = buildHtml(routine, studentDataMap, enrollmentMap);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.setContent(html, new Page.SetContentOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE));
            byte[] pdf = page.pdf(new Page.PdfOptions()
                    .setPrintBackground(true)
                    .setMargin(new Margin().setTop("0").setBottom("0")
                            .setLeft("0").setRight("0"))
                    .setWidth(PAGE_W + "px")
                    .setHeight(PAGE_H + "px"));
            browser.close();
            return pdf;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FLOW 2 — By section (no room)
    // ═══════════════════════════════════════════════════════════════════════

    public byte[] generateAdmitCardsBySection(
            Integer routineId,
            Integer sessionId,
            Integer classId,
            Integer genderSectionId,
            Long sectionId,
            Integer groupId,
            Integer startRoll,
            Integer endRoll
    ) throws Exception {
        AdmitCardRoutineResponseDto routine = admitCardService.getAdmitCardDataBySection(
                routineId, sessionId, classId, genderSectionId, sectionId, groupId);

        List<EnrollmentResponseDto> allEnrollments = fetchEnrollments(
                null, classId, sectionId, null, genderSectionId, groupId, startRoll, endRoll);

        Map<String, EnrollmentResponseDto> enrollmentMap = allEnrollments.stream()
                .filter(e -> e.getStudentSystemId() != null)
                .collect(Collectors.toMap(
                        EnrollmentResponseDto::getStudentSystemId,
                        e -> e,
                        (a, b) -> a));

        Map<String, StudentAdmitDataBySection> studentDataMap = new LinkedHashMap<>();
        for (AdmitCardSessionDto session : routine.getSessions()) {
            for (AdmitCardAllocationDto alloc : session.getAllocations()) {
                for (AdmitCardStudentDto student : alloc.getStudents()) {
                    String sid = student.getStudentSystemId();
                    if (sid == null) continue;
                    studentDataMap.computeIfAbsent(sid, k -> new StudentAdmitDataBySection(
                            sid,
                            student.getClassRoll(),
                            alloc.getSectionName(),
                            alloc.getGenderSectionName(),
                            session.getClassName()
                    )).addSession(session);
                }
            }
        }

        for (Map.Entry<String, StudentAdmitDataBySection> entry : studentDataMap.entrySet()) {
            EnrollmentResponseDto e = enrollmentMap.get(entry.getKey());
            if (e == null) continue;
            StudentAdmitDataBySection d = entry.getValue();
            if (d.sectionName == null && e.getSection() != null)
                d.sectionName = e.getSection().getSectionName();
            if (d.genderSectionName == null && e.getGenderSection() != null)
                d.genderSectionName = e.getGenderSection().getGenderName();
            if (d.groupName == null && e.getStudentGroup() != null)
                d.groupName = e.getStudentGroup().getName();
        }

        for (StudentAdmitDataBySection data : studentDataMap.values()) {
            if (!data.sessions.isEmpty()) {
                List<AdmitCardSessionDto> fullSchedule = data.sessions.get(0).getFullSchedule();
                if (fullSchedule != null && !fullSchedule.isEmpty()) {
                    Integer studentGroupId = getGroupId(enrollmentMap, data.studentSystemId);
                    data.sessions = fullSchedule.stream()
                            .filter(s -> s.getGroupId() == null ||
                                    studentGroupId == null ||
                                    s.getGroupId().equals(studentGroupId))
                            .sorted(Comparator.comparing(AdmitCardSessionDto::getDate))
                            .collect(Collectors.toList());
                }
            }
        }

        if (startRoll != null || endRoll != null) {
            studentDataMap.entrySet().removeIf(entry -> {
                EnrollmentResponseDto e = enrollmentMap.get(entry.getKey());
                if (e == null) return true;
                Integer roll = e.getClassRoll();
                if (roll == null) return true;
                if (startRoll != null && roll < startRoll) return true;
                if (endRoll != null && roll > endRoll) return true;
                return false;
            });
        }

        String html = buildHtmlBySection(routine, studentDataMap, enrollmentMap);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.setContent(html, new Page.SetContentOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE));
            byte[] pdf = page.pdf(new Page.PdfOptions()
                    .setPrintBackground(true)
                    .setMargin(new Margin().setTop("0").setBottom("0")
                            .setLeft("0").setRight("0"))
                    .setWidth(PAGE_W + "px")
                    .setHeight(PAGE_H + "px"));
            browser.close();
            return pdf;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Enrollment helper
    // ═══════════════════════════════════════════════════════════════════════

    private List<EnrollmentResponseDto> fetchEnrollments(
            Integer academicYearId, Integer classId, Long sectionId, Integer shiftId,
            Integer genderSectionId, Integer groupId, Integer startRoll, Integer endRoll
    ) {
        Specification<Enrollment> spec = EnrollmentSpecification.filter(
                academicYearId, classId, sectionId, shiftId, genderSectionId,
                groupId, true, null, startRoll, endRoll);
        return enrollmentRepository.findAll(spec).stream()
                .filter(e -> e.getStudent() != null)
                .map(e -> EnrollmentResponseDto.from(e, e.getStudent()))
                .collect(Collectors.toList());
    }

    private Integer getGroupId(Map<String, EnrollmentResponseDto> enrollmentMap, String studentSystemId) {
        EnrollmentResponseDto e = enrollmentMap.get(studentSystemId);
        if (e == null || e.getStudentGroup() == null) return null;
        return e.getStudentGroup().getId();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Inner data holders
    // ═══════════════════════════════════════════════════════════════════════

    static class StudentAdmitData {
        String studentSystemId;
        Integer classRoll;
        String roomName;
        String sectionName;
        String genderSectionName;
        String className;
        List<AdmitCardSessionDto> sessions = new ArrayList<>();

        StudentAdmitData(String studentSystemId, Integer classRoll,
                         String roomName, String sectionName,
                         String genderSectionName, String className) {
            this.studentSystemId   = studentSystemId;
            this.classRoll         = classRoll;
            this.roomName          = roomName;
            this.sectionName       = sectionName;
            this.genderSectionName = genderSectionName;
            this.className         = className;
        }

        void addSession(AdmitCardSessionDto s) { sessions.add(s); }
    }

    static class StudentAdmitDataBySection {
        String studentSystemId;
        Integer classRoll;
        String sectionName;
        String genderSectionName;
        String groupName;
        String className;
        List<AdmitCardSessionDto> sessions = new ArrayList<>();

        StudentAdmitDataBySection(String studentSystemId, Integer classRoll,
                                  String sectionName, String genderSectionName,
                                  String className) {
            this.studentSystemId   = studentSystemId;
            this.classRoll         = classRoll;
            this.sectionName       = sectionName;
            this.genderSectionName = genderSectionName;
            this.className         = className;
        }

        void addSession(AdmitCardSessionDto s) { sessions.add(s); }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HTML builders — Flow 1 (with room)
    // ═══════════════════════════════════════════════════════════════════════

    private String buildHtml(
            AdmitCardRoutineResponseDto routine,
            Map<String, StudentAdmitData> studentDataMap,
            Map<String, EnrollmentResponseDto> enrollmentMap
    ) {
        List<StudentAdmitData> students = new ArrayList<>(studentDataMap.values());
        StringBuilder pages = new StringBuilder();

        for (int i = 0; i < students.size(); i += 2) {
            StudentAdmitData s1 = students.get(i);
            StudentAdmitData s2 = (i + 1 < students.size()) ? students.get(i + 1) : null;

            EnrollmentResponseDto e1 = enrollmentMap.get(s1.studentSystemId);
            EnrollmentResponseDto e2 = s2 != null ? enrollmentMap.get(s2.studentSystemId) : null;

            pages.append("<div class=\"page\">");
            pages.append(buildCard(routine, s1, e1));
            pages.append("<div class=\"card-divider\"></div>");
            if (s2 != null) pages.append(buildCard(routine, s2, e2));
            pages.append("</div>");
        }

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>"
                + getCss()
                + "</style></head><body>"
                + pages
                + "</body></html>";
    }

    private String buildCard(
            AdmitCardRoutineResponseDto routine,
            StudentAdmitData data,
            EnrollmentResponseDto enrollment
    ) {
        String name      = enrollment != null ? nvl(enrollment.getNameEnglish(), "N/A") : "N/A";
        String phone     = enrollment != null ? nvl(enrollment.getMotherPhone(), "N/A") : "N/A";
        String groupName = (enrollment != null && enrollment.getStudentGroup() != null)
                ? enrollment.getStudentGroup().getName() : "N/A";

        String photoUrl = (enrollment != null
                && enrollment.getImage() != null
                && Boolean.TRUE.equals(enrollment.getImage().getIsActive())
                && enrollment.getImage().getImageUrl() != null)
                ? idCardPdfService.fetchAndCompressToBase64(enrollment.getImage().getImageUrl())
                : "";

        String photoTag = photoUrl.isEmpty()
                ? "<div class=\"photo-placeholder\">PHOTO</div>"
                : "<img src=\"" + photoUrl + "\" style=\"width:100%;height:100%;object-fit:cover;display:block;\">";

        String logoTag = logoBase64.isEmpty()
                ? "<div style=\"font-size:12px;color:#888;\">LOGO</div>"
                : "<img src=\"" + logoBase64 + "\" style=\"width:68px;height:68px;object-fit:contain;\">";

        String sectionLabel = nvl(data.genderSectionName, "");
        if (data.sectionName != null && !data.sectionName.isBlank())
            sectionLabel += (sectionLabel.isEmpty() ? "" : " - ") + data.sectionName;

        List<AdmitCardSessionDto> sessions = new ArrayList<>(data.sessions);
        sessions.sort(Comparator.comparing(AdmitCardSessionDto::getDate));
        int total     = sessions.size();
        int leftCount = Math.min((int) Math.ceil(total / 2.0), 10);
        List<AdmitCardSessionDto> left  = sessions.subList(0, leftCount);
        List<AdmitCardSessionDto> right = sessions.subList(leftCount, sessions.size());

        String examLabel = nvl(routine.getTitle(), "EXAMINATION");
        if (routine.getAcademicYearName() != null && !routine.getAcademicYearName().isBlank())
            examLabel += " — " + routine.getAcademicYearName();

        String sessionLabel = nvl(data.className, "");
        if (!sectionLabel.isEmpty())
            sessionLabel += (sessionLabel.isEmpty() ? "" : " | ") + sectionLabel;
        if (groupName != null && !groupName.equals("N/A"))
            sessionLabel += (sessionLabel.isEmpty() ? "" : " | ") + groupName;

        return "<div class=\"admit-card\">"
                + "<div class=\"ac-header\">"
                + "<div class=\"ac-logo\">" + logoTag + "</div>"
                + "<div class=\"ac-header-center\">"
                + "<div class=\"ac-arabic\">بسم الله الرحمن الرحيم</div>"
                + "<div class=\"ac-school\">LUTFUR RAHMAN ALIM MADRASAH</div>"
                + "<div class=\"ac-address\">LUTFUR RAHMAN ROAD, NATULLABAD, BARISHAL</div>"
                + "</div>"
                + "<div class=\"ac-photo\">" + photoTag + "</div>"
                + "</div>"
                + "<hr class=\"ac-divider-line\">"
                + "<div class=\"ac-exam-section\">"
                + "<div class=\"ac-exam-box\">" + examLabel + "</div>"
                + "<div class=\"ac-session-info\">" + sessionLabel + "</div>"
                + "<div class=\"ac-admit-badge\">ADMIT CARD</div>"
                + "</div>"
                + "<hr class=\"ac-divider-line\">"
                + "<div class=\"ac-info\">"
                + "<table class=\"info-table\">"
                + infoRow("Student ID",     data.studentSystemId)
                + infoRow("Student's Name", "<b>" + name + "</b>")
                + "</table>"
                + "<table class=\"info-table\">"
                + infoRow("Roll",          data.classRoll != null ? data.classRoll.toString() : "N/A")
                + infoRow("Parent Mobile", phone)
                + "</table>"
                + "</div>"
                + "<div class=\"ac-tables\">"
                + buildScheduleTable(left,  data.roomName)
                + buildScheduleTable(right, data.roomName)
                + "</div>"
                + "<div class=\"ac-bottom\">"
                + "<div class=\"ac-footer\">"
                + "<div class=\"ac-instruction\"><b>Instruction:</b>"
                + "<ol style=\"margin:4px 0 0 16px;padding:0;\">"
                + "<li>Examinees must enter the exam hall at least 15 minutes before the exam starts.</li>"
                + "</ol></div>"
                + "<div class=\"sig-group\">"
                + "<div class=\"sig\">_________________<br>Class Teacher</div>"
                + (signatureBase64.isEmpty()
                    ? "<div class=\"sig\">_________________<br>Principal</div>"
                    : "<div class=\"sig\"><img src=\"" + signatureBase64
                        + "\" style=\"height:28px;object-fit:contain;\"><br>"
                        + "<div style=\"border-top:1px solid #333;padding-top:2px;\">Principal</div></div>")
                + "</div>"
                + "</div>"
                + "</div>"
                + "</div>";
    }

    private String buildScheduleTable(List<AdmitCardSessionDto> sessions, String roomName) {
        if (sessions.isEmpty()) return "<table class=\"schedule\"></table>";
        StringBuilder rows = new StringBuilder();
        for (AdmitCardSessionDto s : sessions) {
            rows.append("<tr>")
                    .append("<td>").append(formatDate(s.getDate())).append("</td>")
                    .append("<td>").append(nvl(s.getSubjectName(), "")).append("</td>")
                    .append("<td>").append(formatTime(s.getStartTime()))
                    .append(" - ").append(formatTime(s.getEndTime())).append("</td>")
                    .append("<td>").append(nvl(roomName, "N/A")).append("</td>")
                    .append("</tr>");
        }
        return "<table class=\"schedule\">"
                + "<tr><th>Date</th><th>Subject</th><th>Time</th><th>Room</th></tr>"
                + rows
                + "</table>";
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HTML builders — Flow 2 (no room)
    // ═══════════════════════════════════════════════════════════════════════

    private String buildHtmlBySection(
            AdmitCardRoutineResponseDto routine,
            Map<String, StudentAdmitDataBySection> studentDataMap,
            Map<String, EnrollmentResponseDto> enrollmentMap
    ) {
        List<StudentAdmitDataBySection> students = new ArrayList<>(studentDataMap.values());
        StringBuilder pages = new StringBuilder();

        for (int i = 0; i < students.size(); i += 2) {
            StudentAdmitDataBySection s1 = students.get(i);
            StudentAdmitDataBySection s2 = (i + 1 < students.size()) ? students.get(i + 1) : null;

            EnrollmentResponseDto e1 = enrollmentMap.get(s1.studentSystemId);
            EnrollmentResponseDto e2 = s2 != null ? enrollmentMap.get(s2.studentSystemId) : null;

            pages.append("<div class=\"page\">");
            pages.append(buildCardBySection(routine, s1, e1));
            pages.append("<div class=\"card-divider\"></div>");
            if (s2 != null) pages.append(buildCardBySection(routine, s2, e2));
            pages.append("</div>");
        }

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>"
                + getCss()
                + "</style></head><body>"
                + pages
                + "</body></html>";
    }

    private String buildCardBySection(
            AdmitCardRoutineResponseDto routine,
            StudentAdmitDataBySection data,
            EnrollmentResponseDto enrollment
    ) {
        String name      = enrollment != null ? nvl(enrollment.getNameEnglish(), "N/A") : "N/A";
        String phone     = enrollment != null ? nvl(enrollment.getMotherPhone(), "N/A") : "N/A";
        String groupName = nvl(data.groupName, "N/A");

        String photoUrl = (enrollment != null
                && enrollment.getImage() != null
                && Boolean.TRUE.equals(enrollment.getImage().getIsActive())
                && enrollment.getImage().getImageUrl() != null)
                ? idCardPdfService.fetchAndCompressToBase64(enrollment.getImage().getImageUrl())
                : "";

        String photoTag = photoUrl.isEmpty()
                ? "<div class=\"photo-placeholder\">PHOTO</div>"
                : "<img src=\"" + photoUrl + "\" style=\"width:100%;height:100%;object-fit:cover;display:block;\">";

        String logoTag = logoBase64.isEmpty()
                ? "<div style=\"font-size:12px;color:#888;\">LOGO</div>"
                : "<img src=\"" + logoBase64 + "\" style=\"width:68px;height:68px;object-fit:contain;\">";

        String sectionLabel = nvl(data.genderSectionName, "");
        if (data.sectionName != null && !data.sectionName.isBlank())
            sectionLabel += (sectionLabel.isEmpty() ? "" : " - ") + data.sectionName;

        List<AdmitCardSessionDto> sessions = new ArrayList<>(data.sessions);
        sessions.sort(Comparator.comparing(AdmitCardSessionDto::getDate));
        int leftCount = Math.min((int) Math.ceil(sessions.size() / 2.0), 10);
        var left  = sessions.subList(0, leftCount);
        var right = sessions.subList(leftCount, sessions.size());

        String examLabel = nvl(routine.getTitle(), "EXAMINATION");
        if (routine.getAcademicYearName() != null && !routine.getAcademicYearName().isBlank())
            examLabel += " — " + routine.getAcademicYearName();

        String sessionLabel = nvl(data.className, "");
        if (!sectionLabel.isEmpty())
            sessionLabel += (sessionLabel.isEmpty() ? "" : " | ") + sectionLabel;
        if (groupName != null && !groupName.equals("N/A"))
            sessionLabel += (sessionLabel.isEmpty() ? "" : " | ") + groupName;

        return "<div class=\"admit-card\">"
                + "<div class=\"ac-header\">"
                + "<div class=\"ac-logo\">" + logoTag + "</div>"
                + "<div class=\"ac-header-center\">"
                + "<div class=\"ac-arabic\">بسم الله الرحمن الرحيم</div>"
                + "<div class=\"ac-school\">LUTFUR RAHMAN ALIM MADRASAH</div>"
                + "<div class=\"ac-address\">LUTFUR RAHMAN ROAD, NATULLABAD, BARISHAL</div>"
                + "</div>"
                + "<div class=\"ac-photo\">" + photoTag + "</div>"
                + "</div>"
                + "<hr class=\"ac-divider-line\">"
                + "<div class=\"ac-exam-section\">"
                + "<div class=\"ac-exam-box\">" + examLabel + "</div>"
                + "<div class=\"ac-session-info\">" + sessionLabel + "</div>"
                + "<div class=\"ac-admit-badge\">ADMIT CARD</div>"
                + "</div>"
                + "<hr class=\"ac-divider-line\">"
                + "<div class=\"ac-info\">"
                + "<table class=\"info-table\">"
                + infoRow("Student ID",     data.studentSystemId)
                + infoRow("Student's Name", "<b>" + name + "</b>")
                + "</table>"
                + "<table class=\"info-table\">"
                + infoRow("Roll",          data.classRoll != null ? data.classRoll.toString() : "N/A")
                + infoRow("Parent Mobile", phone)
                + "</table>"
                + "</div>"
                + "<div class=\"ac-tables\">"
                + buildScheduleTableNoRoom(left)
                + buildScheduleTableNoRoom(right)
                + "</div>"
                + "<div class=\"ac-bottom\">"
                + "<div class=\"ac-footer\">"
                + "<div class=\"ac-instruction\"><b>Instruction:</b>"
                + "<ol style=\"margin:4px 0 0 16px;padding:0;\">"
                + "<li>Examinees must enter the exam hall at least 15 minutes before the exam starts.</li>"
                + "</ol></div>"
                + "<div class=\"sig-group\">"
                + "<div class=\"sig\">_________________<br>Class Teacher</div>"
                + (signatureBase64.isEmpty()
                    ? "<div class=\"sig\">_________________<br>Principal</div>"
                    : "<div class=\"sig\"><img src=\"" + signatureBase64
                        + "\" style=\"height:28px;object-fit:contain;\"><br>"
                        + "<div style=\"border-top:1px solid #333;padding-top:2px;\">Principal</div></div>")
                + "</div>"
                + "</div>"
                + "</div>"
                + "</div>";
    }

    private String buildScheduleTableNoRoom(List<AdmitCardSessionDto> sessions) {
        if (sessions.isEmpty()) return "<table class=\"schedule\"></table>";
        StringBuilder rows = new StringBuilder();
        for (AdmitCardSessionDto s : sessions) {
            rows.append("<tr>")
                    .append("<td>").append(formatDate(s.getDate())).append("</td>")
                    .append("<td>").append(nvl(s.getSubjectName(), "")).append("</td>")
                    .append("<td>").append(formatTime(s.getStartTime()))
                    .append(" - ").append(formatTime(s.getEndTime())).append("</td>")
                    .append("</tr>");
        }
        return "<table class=\"schedule\">"
                + "<tr><th>Date</th><th>Subject</th><th>Time</th></tr>"
                + rows
                + "</table>";
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Shared helpers
    // ═══════════════════════════════════════════════════════════════════════

    private String infoRow(String label, String value) {
        return "<tr><td class=\"lbl\">" + label + "</td>"
                + "<td class=\"sep\">:</td>"
                + "<td class=\"val\">" + value + "</td></tr>";
    }

    private String nvl(String s, String fallback) {
        return (s != null && !s.isBlank()) ? s : fallback;
    }

    private String formatDate(String date) {
        if (date == null) return "";
        try {
            String[] p = date.split("-");
            return p[2] + "-" + p[1] + "-" + p[0];
        } catch (Exception e) { return date; }
    }

    private String formatTime(String time) {
        if (time == null) return "";
        try {
            String[] p    = time.split(":");
            int hour      = Integer.parseInt(p[0]);
            String min    = p[1];
            String period = hour >= 12 ? "PM" : "AM";
            int h12       = hour > 12 ? hour - 12 : hour == 0 ? 12 : hour;
            return h12 + ":" + min + " " + period;
        } catch (Exception e) { return time; }
    }

    private String getCss() {
        return "* { box-sizing: border-box; margin: 0; padding: 0; }"
                + "body { background: #fff; font-family: Arial, sans-serif; }"
                + ".page { width: 794px; height: 1123px; display: flex;"
                + "  flex-direction: column; padding: 16px 20px; gap: 10px; overflow: hidden; }"
                + ".card-divider { display: none; }"
                + ".admit-card { width: 100%; height: 540px; flex-shrink: 0;"
                + "  border: 7px solid #6ec1e4; padding: 12px 14px;"
                + "  position: relative; background: #fff;"
                + "  display: flex; flex-direction: column; overflow: hidden; }"
                + ".admit-card::before { content:''; position:absolute; top:7px; left:7px;"
                + "  right:7px; bottom:7px; border:3px solid #f4c542; pointer-events:none; }"
                + ".ac-header { display:flex; align-items:center; gap:10px;"
                + "  padding-bottom:8px; flex-shrink:0; }"
                + ".ac-logo { width:64px; height:64px; flex-shrink:0; display:flex;"
                + "  align-items:center; justify-content:center; }"
                + ".ac-header-center { flex:1; text-align:center; }"
                + ".ac-arabic { font-size:15px; margin-bottom:2px; }"
                + ".ac-school { font-size:18px; font-weight:bold; margin-bottom:2px; }"
                + ".ac-address { font-size:12px; color:#333; }"
                + ".ac-photo { width:68px; height:82px; border:1px solid #ccc; flex-shrink:0;"
                + "  overflow:hidden; display:flex; align-items:center; justify-content:center; }"
                + ".photo-placeholder { font-size:11px; color:#888; text-align:center; }"
                + ".ac-divider-line { border:none; border-top:1px solid #ccc; margin:0; flex-shrink:0; }"
                + ".ac-exam-section { text-align:center; margin:8px 0; flex-shrink:0; }"
                + ".ac-exam-box { display:inline-block; border:2px solid #6ec1e4;"
                + "  padding:4px 16px; font-weight:bold; font-size:14px; margin-bottom:4px; }"
                + ".ac-session-info { font-size:13px; color:#444; margin-bottom:6px; font-weight:600; }"
                + ".ac-admit-badge { display:inline-block; background:#6ec1e4; color:#fff;"
                + "  font-weight:bold; font-size:13px; padding:3px 24px; }"
                + ".ac-info { display:flex; gap:12px; padding:14px 0; flex-shrink:0; }"
                + ".info-table { border-collapse:collapse; font-size:12px; flex:1; }"
                + ".info-table td { padding:3px 4px; }"
                + ".info-table td.lbl { font-weight:bold; white-space:nowrap; }"
                + ".info-table td.sep { width:6px; }"
                + ".ac-tables { display:flex; gap:6px; flex:1; min-height:0; margin-top:4px;"
                + "  align-items:flex-start; }"
                + ".schedule { width:50%; border-collapse:collapse; font-size:10px;"
                + "  table-layout:fixed; }"
                + ".schedule th, .schedule td { border:1px solid #333; padding:3px 4px;"
                + "  text-align:center; overflow:hidden; white-space:nowrap; text-overflow:ellipsis; }"
                + ".schedule th:nth-child(1), .schedule td:nth-child(1) { width:20%; }"
                + ".schedule th:nth-child(2), .schedule td:nth-child(2) { width:31%; }"
                + ".schedule th:nth-child(3), .schedule td:nth-child(3) { width:33%; }"
                + ".schedule th:nth-child(4), .schedule td:nth-child(4) { width:16%; }"
                + ".schedule th { background:#eee; }"
                + ".ac-bottom { margin-top:auto; flex-shrink:0; }"
                + ".ac-footer { display:flex; justify-content:space-between; align-items:flex-end;"
                + "  font-size:13px; padding:0 4px; gap:12px; }"
                + ".ac-instruction { font-size:12px; flex:1; }"
                + ".sig-group { display:flex; gap:24px; align-items:flex-end; flex-shrink:0; }"
                + ".sig { text-align:center; font-size:12px; }";
    }
}
