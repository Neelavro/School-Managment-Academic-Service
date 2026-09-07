package com.example.academic_service.service;

import com.example.academic_service.dto.result_dtos.ProgressReportData;
import com.example.academic_service.entity.Grade;
import com.example.academic_service.entity.GradingPolicy;
import com.example.academic_service.entity.SystemSettings;
import com.example.academic_service.repository.GradingPolicyRepository;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Margin;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressReportPdfService {

    private final ResultService resultService;
    private final SystemSettingsService systemSettingsService;
    private final GradingPolicyRepository gradingPolicyRepository;
    private final IdCardPdfService idCardPdfService;

    private static final int PAGE_W = 794;
    private static final int PAGE_H = 1123;

    public byte[] generate(Integer examRoutineId, Integer classId, Integer shiftId,
                           Integer genderSectionId, Long sectionId, Integer groupId,
                           Integer startRoll, Integer endRoll) throws Exception {

        // Partition by group so the PDF renders one group at a time with its
        // own subject set (e.g. Science students with Science columns, then
        // Humanities students with Humanities columns).
        List<ProgressReportData> partitions = resultService.getProgressReportDataPartitioned(
                examRoutineId, classId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll);

        SystemSettings settings = systemSettingsService.getSettings();
        String institutionName = nvl(settings.getInstitutionName(), "");
        String address         = nvl(settings.getAddress(), "");
        String heading         = settings.getHeading();
        String logoBase64      = resolveBase64(settings.getLogoUrl());
        String signatureBase64 = resolveBase64(settings.getSignatureUrl());

        List<Grade> gradingTable = loadGradingTable(classId);

        // Build all student pages across every partition into one single
        // <html> document (don't nest html docs by calling buildHtml twice).
        StringBuilder pages = new StringBuilder();
        for (ProgressReportData part : partitions) {
            for (ProgressReportData.StudentReport student : part.getStudents()) {
                pages.append(buildPage(part, student, institutionName, address, heading,
                        logoBase64, signatureBase64, gradingTable));
            }
        }
        String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>"
                + getCss()
                + "</style></head><body>"
                + pages
                + "</body></html>";

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.setContent(html, new Page.SetContentOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
            byte[] pdf = page.pdf(new Page.PdfOptions()
                    .setPrintBackground(true)
                    .setMargin(new Margin().setTop("0").setBottom("0").setLeft("0").setRight("0"))
                    .setWidth(PAGE_W + "px")
                    .setHeight(PAGE_H + "px"));
            browser.close();
            return pdf;
        }
    }

    private String buildHtml(ProgressReportData data, String institutionName, String address,
                              String heading, String logoBase64, String signatureBase64,
                              List<Grade> gradingTable) {
        StringBuilder pages = new StringBuilder();

        for (ProgressReportData.StudentReport student : data.getStudents()) {
            pages.append(buildPage(data, student, institutionName, address, heading,
                    logoBase64, signatureBase64, gradingTable));
        }

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>"
                + getCss()
                + "</style></head><body>"
                + pages
                + "</body></html>";
    }

    private String buildPage(ProgressReportData data, ProgressReportData.StudentReport student,
                              String institutionName, String address, String heading,
                              String logoBase64, String signatureBase64, List<Grade> gradingTable) {

        String photoTag = buildPhotoTag(student.getImageUrl());
        String logoTag  = logoBase64.isEmpty()
                ? "<div class=\"logo\">LOGO</div>"
                : "<img src=\"" + logoBase64 + "\" style=\"width:18mm;height:18mm;object-fit:contain;border-radius:50%;border:1.5px solid #0F6E56;\">";

        String classLabel = data.getClassName();
        String sectionParts = joinNonBlank(" | ",
                student.getGenderSectionName(), student.getSectionName());
        if (!sectionParts.isEmpty()) classLabel += " — " + sectionParts;

        // Header
        String headerHtml = "<div class=\"header\">"
                + (heading != null && !heading.isBlank() ? "<div class=\"bismillah\">" + heading + "</div>" : "")
                + "<div class=\"madrasah-name\">" + institutionName + "</div>"
                + "<div class=\"address\">" + address + "</div>"
                + "</div>";

        // Grading table
        String gradingHtml = buildGradingTableHtml(gradingTable);

        // ID row
        String idRowHtml = "<div class=\"id-row\">"
                + "<div class=\"photo\">" + photoTag + "</div>"
                + "<div class=\"logo-stack\">"
                + "<div class=\"logo-wrap\">" + logoTag + "</div>"
                + "<div class=\"stamp\">PROGRESS REPORT</div>"
                + "<div class=\"stamp-sub\">" + joinNonBlank(" - ", nvl(data.getRoutineTitle(), ""), nvl(data.getAcademicYearName(), "")) + "</div>"
                + "</div>"
                + gradingHtml
                + "</div>";

        // Phone: mother first, fall back to father
        String contactPhone = nvl(student.getMotherPhone(), null) != null
                ? student.getMotherPhone()
                : nvl(student.getFatherPhone(), "N/A");

        // Info grid
        String infoHtml = "<div class=\"info-grid\">"
                + "<table>"
                + infoRow("Name of Student", nvl(student.getStudentName(), "N/A"), "lbl-d", "v-blue")
                + infoRow("Phone No.",       contactPhone, "lbl-g", "v-blue")
                + infoRow("Student ID",      nvl(student.getStudentSystemId(), "N/A"), "lbl-p", "v-blue")
                + infoRow("Roll No.",        student.getClassRoll() != null ? String.valueOf(student.getClassRoll()) : "N/A", "lbl-p", "v-blue")
                + infoRow("Class",           classLabel, "lbl-b", "v-blue")
                + "</table>"
                + "<table>"
                + (student.getGroupName() != null ? infoRow("Group", student.getGroupName(), "lbl-r", "v-red") : "")
                + "</table>"
                + "</div>";

        // Marks table
        String marksHtml = buildMarksTable(data, student, gradingTable);

        // Result summary
        String resultHtml = buildResultSummary(student);

        // Behavior/co-curricular
        String checksHtml = buildChecksHtml(student.getOverallGpa(), gradingTable);

        // Signatures
        String sigHtml = "<div class=\"signatures\">"
                + "<div><div class=\"sig-line\">CLASS TEACHER</div></div>"
                + "<div>"
                + (signatureBase64.isEmpty() ? "" : "<div class=\"sig-name\" style=\"font-family:'Brush Script MT',cursive;font-style:italic;color:#013E5B;font-size:14pt;margin-bottom:1mm;\">&nbsp;</div>")
                + (signatureBase64.isEmpty() ? "" : "<img src=\"" + signatureBase64 + "\" style=\"height:28px;object-fit:contain;display:block;margin:0 auto;\">")
                + "<div class=\"sig-line\">PRINCIPAL</div>"
                + "</div>"
                + "</div>";

        // Footer
        String footerHtml = "<div class=\"footer\">"
                + "<div>Powered by Technonix</div>"
                + "<div>Date of Publication: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) + "</div>"
                + "</div>";

        return "<div class=\"page\">"
                + "<div class=\"inner\">"
                + "<div class=\"content-area\">"
                + headerHtml
                + idRowHtml
                + infoHtml
                + marksHtml
                + resultHtml
                + checksHtml
                + "</div>"
                + "<div class=\"bottom-area\">"
                + sigHtml
                + footerHtml
                + "</div>"
                + "</div>"
                + "</div>";
    }

    private String buildMarksTable(ProgressReportData data, ProgressReportData.StudentReport student,
                                   List<Grade> gradingTable) {
        List<ProgressReportData.ComponentInfo> components = data.getComponents();
        List<ProgressReportData.SubjectInfo>   subjects   = data.getSubjects();

        Map<Integer, ProgressReportData.SubjectResult> resultMap = new HashMap<>();
        for (ProgressReportData.SubjectResult sr : student.getSubjectResults()) {
            resultMap.put(sr.getSubjectId(), sr);
        }

        // Group merged subjects by mergeGroupId (preserving encounter order)
        Map<Integer, List<ProgressReportData.SubjectInfo>> byMergeGroup = new LinkedHashMap<>();
        for (ProgressReportData.SubjectInfo si : subjects) {
            if (si.getMergeGroupId() != null) {
                byMergeGroup.computeIfAbsent(si.getMergeGroupId(), k -> new ArrayList<>()).add(si);
            }
        }

        int fixedCols  = 6;
        int compCols   = components.size();
        int totalCols  = fixedCols + compCols;
        int subjectPct = 26;
        int eachOther  = (100 - subjectPct) / (totalCols - 1);
        int remainder  = 100 - subjectPct - eachOther * (totalCols - 1);

        StringBuilder colgroup = new StringBuilder("<colgroup>");
        colgroup.append("<col style=\"width:").append(subjectPct + remainder).append("%\">");
        for (int i = 1; i < totalCols; i++) colgroup.append("<col style=\"width:").append(eachOther).append("%\">");
        colgroup.append("</colgroup>");

        String compHeaders1 = "<th colspan=\"" + components.size() + "\">Obtaining Marks</th>";
        StringBuilder compHeaders2 = new StringBuilder();
        for (ProgressReportData.ComponentInfo c : components) {
            compHeaders2.append("<th style=\"font-size:7.5pt;\">").append(c.getComponentName()).append("</th>");
        }

        String thead = "<thead>"
                + "<tr class=\"th-tan\">"
                + "<th rowspan=\"2\">Name of Subjects</th>"
                + "<th rowspan=\"2\">Full<br>Marks</th>"
                + "<th rowspan=\"2\">Highest<br>Marks</th>"
                + compHeaders1
                + "<th rowspan=\"2\">Total<br>Marks</th>"
                + "<th rowspan=\"2\">Letter<br>Grade</th>"
                + "<th rowspan=\"2\">Grade<br>Point</th>"
                + "</tr>"
                + "<tr class=\"th-tan\" style=\"font-weight:400;\">"
                + compHeaders2
                + "</tr>"
                + "</thead>";

        StringBuilder tbody = new StringBuilder("<tbody>");
        BigDecimal totalFull     = BigDecimal.ZERO;
        BigDecimal totalObtained = BigDecimal.ZERO;
        Set<Integer> processedMergeGroups = new HashSet<>();

        for (ProgressReportData.SubjectInfo si : subjects) {
            Integer mgId = si.getMergeGroupId();

            if (mgId != null) {
                if (processedMergeGroups.contains(mgId)) continue;
                processedMergeGroups.add(mgId);

                List<ProgressReportData.SubjectInfo> pair = byMergeGroup.get(mgId);
                int rowCount = pair.size();

                // Compute combined obtained/full for the merged group grade
                BigDecimal combinedObtained = BigDecimal.ZERO;
                int combinedFull = 0;
                boolean anyAppeared = false;
                for (ProgressReportData.SubjectInfo msi : pair) {
                    ProgressReportData.SubjectResult msr = resultMap.get(msi.getSubjectId());
                    if (msr != null && msr.isAppeared()) {
                        anyAppeared = true;
                        if (msr.getTotalMarks() != null)
                            combinedObtained = combinedObtained.add(msr.getTotalMarks());
                    }
                    if (msi.getTotalMarks() != null) combinedFull += msi.getTotalMarks();
                }
                String combinedGrade = anyAppeared ? resolveMergedGrade(combinedObtained, combinedFull, gradingTable) : "—";
                String combinedGpa   = anyAppeared ? resolveMergedGpa(combinedObtained, combinedFull, gradingTable) : "—";

                for (int i = 0; i < rowCount; i++) {
                    ProgressReportData.SubjectInfo msi = pair.get(i);
                    ProgressReportData.SubjectResult msr = resultMap.get(msi.getSubjectId());
                    boolean appeared = msr != null && msr.isAppeared();
                    String totalCell = appeared && msr.getTotalMarks() != null ? fmtMark(msr.getTotalMarks()) : "—";
                    String highCell  = fmtMark(msi.getHighestMarks());

                    String rowStyle = i == 0 ? " class=\"merged-first\"" : " class=\"merged-last\"";
                    tbody.append("<tr").append(rowStyle).append(">");
                    tbody.append("<td class=\"subject\">").append(msi.getSubjectName()).append("</td>");
                    tbody.append("<td>").append(msi.getTotalMarks()).append("</td>");
                    tbody.append("<td>").append(highCell).append("</td>");
                    for (ProgressReportData.ComponentInfo c : components) {
                        BigDecimal cm = msr != null ? msr.getComponentMarks().get(c.getComponentId()) : null;
                        tbody.append("<td>").append(cm != null ? fmtMark(cm) : "—").append("</td>");
                    }
                    tbody.append("<td>").append(totalCell).append("</td>");
                    if (i == 0) {
                        tbody.append("<td rowspan=\"").append(rowCount).append("\" class=\"merged-grade\">").append(combinedGrade).append("</td>");
                        tbody.append("<td rowspan=\"").append(rowCount).append("\" class=\"merged-grade\">").append(combinedGpa).append("</td>");
                    }
                    tbody.append("</tr>");

                    // Totals include every subject on the card — 4th included
                    // — to stay consistent with the with-4th overall GPA shown
                    // in the footer.
                    totalFull = totalFull.add(BigDecimal.valueOf(msi.getTotalMarks() != null ? msi.getTotalMarks() : 0));
                    if (appeared && msr.getTotalMarks() != null) totalObtained = totalObtained.add(msr.getTotalMarks());
                }

            } else {
                ProgressReportData.SubjectResult sr = resultMap.get(si.getSubjectId());
                // 4th-subject row visibility: a class-level 4th option only
                // belongs on the card of students who picked it (via override)
                // or have it in their compulsory junction. Students who didn't
                // pick it would otherwise get an empty 4th row — skip them.
                if (si.isFourthSubject() && sr == null) {
                    continue;
                }
                boolean appeared = sr != null && sr.isAppeared();
                String totalCell = appeared && sr.getTotalMarks() != null ? fmtMark(sr.getTotalMarks()) : "—";
                String gradeCell = appeared && sr.getGradeName() != null ? sr.getGradeName() : "—";
                String gpaCell   = appeared && sr.getGpaValue()  != null ? fmtGpa(sr.getGpaValue()) : "—";
                String highCell  = fmtMark(si.getHighestMarks());

                // (4th) badge is per-student: shown only on this student's
                // actual 4th pick, not on every subject the class declares
                // as a 4th option.
                boolean isStudentFourth = sr != null && sr.isFourthSubject();
                tbody.append("<tr>");
                tbody.append("<td class=\"subject\">").append(si.getSubjectName())
                        .append(isStudentFourth ? " (4<sup>th</sup>)" : "").append("</td>");
                tbody.append("<td>").append(si.getTotalMarks()).append("</td>");
                tbody.append("<td>").append(highCell).append("</td>");
                for (ProgressReportData.ComponentInfo c : components) {
                    BigDecimal cm = sr != null ? sr.getComponentMarks().get(c.getComponentId()) : null;
                    tbody.append("<td>").append(cm != null ? fmtMark(cm) : "—").append("</td>");
                }
                tbody.append("<td>").append(totalCell).append("</td>");
                tbody.append("<td>").append(gradeCell).append("</td>");
                tbody.append("<td>").append(gpaCell).append("</td>");
                tbody.append("</tr>");

                // Totals include every subject on the card — 4th included —
                // matching the with-4th overall GPA shown in the footer.
                totalFull = totalFull.add(BigDecimal.valueOf(si.getTotalMarks() != null ? si.getTotalMarks() : 0));
                if (appeared && sr.getTotalMarks() != null) totalObtained = totalObtained.add(sr.getTotalMarks());
            }
        }
        tbody.append("</tbody>");

        // Footer
        String overallGrade = resolveGradeNameFromGpa(student.getOverallGpa(), data);
        String gpaStr = student.getOverallGpa() != null ? fmtGpa(student.getOverallGpa()) : "—";
        int compColspan = components.size() + 1; // Highest col + all component cols
        String tfoot = "<tfoot>"
                + "<tr>"
                + "<td style=\"color:#0F6E56;\">Total Exam Marks</td>"
                + "<td style=\"color:#013E5B;text-align:center;\">" + totalFull + "</td>"
                + "<td colspan=\"" + compColspan + "\" style=\"color:#0F6E56;text-align:center;\">Obtained Marks &amp; GPA</td>"
                + "<td style=\"color:#C2185B;text-align:center;\">" + fmtMark(totalObtained) + "</td>"
                + "<td style=\"color:#C2185B;text-align:center;\">" + overallGrade + "</td>"
                + "<td style=\"color:#C2185B;text-align:center;\">" + gpaStr + "</td>"
                + "</tr>"
                + "</tfoot>";

        return "<table class=\"marks\">" + colgroup + thead + tbody + tfoot + "</table>";
    }

    private String resolveMergedGrade(BigDecimal obtained, int fullMarks, List<Grade> gradingTable) {
        if (fullMarks <= 0 || gradingTable.isEmpty()) return "—";
        double pct = obtained.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(fullMarks), 4, java.math.RoundingMode.HALF_UP)
                .doubleValue();
        return gradingTable.stream()
                .filter(g -> pct >= g.getMinMark())
                .findFirst()
                .map(Grade::getName)
                .orElse("F");
    }

    private String resolveMergedGpa(BigDecimal obtained, int fullMarks, List<Grade> gradingTable) {
        if (fullMarks <= 0 || gradingTable.isEmpty()) return "—";
        double pct = obtained.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(fullMarks), 4, java.math.RoundingMode.HALF_UP)
                .doubleValue();
        return gradingTable.stream()
                .filter(g -> pct >= g.getMinMark())
                .findFirst()
                .map(g -> fmtGpa(g.getGpaValue()))
                .orElse("0.00");
    }

    private String resolveGradeNameFromGpa(Double gpa, ProgressReportData data) {
        if (gpa == null) return "—";
        // best approximation: highest grade whose gpaValue <= overallGpa
        // We don't have the full grade table here, so just return based on gpa ranges
        if (gpa >= 5.0) return "A+";
        if (gpa >= 4.0) return "A";
        if (gpa >= 3.5) return "A-";
        if (gpa >= 3.0) return "B";
        if (gpa >= 2.0) return "C";
        if (gpa >= 1.0) return "D";
        return "F";
    }

    private String buildResultSummary(ProgressReportData.StudentReport student) {
        String resultStatus = student.isPassed() ? "Passed" : "Failed";
        String gpaNo4th = student.getGpaWithout4th() != null ? fmtGpa(student.getGpaWithout4th()) : "—";
        String classPos  = student.getClassRank()         != null ? String.valueOf(student.getClassRank())         : "—";
        String shiftPos  = student.getGenderSectionRank() != null ? String.valueOf(student.getGenderSectionRank()) : "—";
        String secPos    = student.getSectionRank()       != null ? String.valueOf(student.getSectionRank())       : "—";

        return "<div class=\"result-row\">"
                + "<table>"
                + resultSummaryRow("Result Status",     resultStatus, "lbl-r", "v-blue")
                + resultSummaryRow("GPA (Without 4th)", gpaNo4th,     "lbl-r", "v-blue")
                + resultSummaryRow("Failed Subject(s)", String.valueOf(student.getFailedSubjectCount()), "lbl-r", "v-blue")
                + "</table>"
                + "<table>"
                + resultSummaryRow("Class Position",   classPos, "lbl-p", "v-blue")
                + resultSummaryRow("Shift Position",   shiftPos, "lbl-p", "v-blue")
                + resultSummaryRow("Section Position", secPos,   "lbl-p", "v-blue")
                + "</table>"
                + "<table>"
                + resultSummaryRow("Total Working Days", "", "lbl-g", "v-blue")
                + resultSummaryRow("Total Present",      "", "lbl-g", "v-blue")
                + resultSummaryRow("Total Absent",       "", "lbl-g", "v-blue")
                + "</table>"
                + "</div>";
    }

    private String buildChecksHtml(Double overallGpa, List<Grade> gradingTable) {
        String comment = "";
        if (overallGpa != null && !gradingTable.isEmpty()) {
            // gradingTable is sorted by minMark desc (A+ first), gpaValue also descending
            comment = gradingTable.stream()
                    .filter(g -> g.getGpaValue() != null && overallGpa >= g.getGpaValue() - 0.001)
                    .findFirst()
                    .map(g -> g.getComment() != null ? g.getComment() : "")
                    .orElse("");
        }
        return "<table class=\"checks\">"
                + "<tr class=\"th-tan\">"
                + "<th colspan=\"4\" style=\"text-align:left;\">Moral &amp; Behavior Evaluation</th>"
                + "<th colspan=\"4\" style=\"text-align:left;\">Co-Curricular Activities</th>"
                + "</tr>"
                + "<tr style=\"text-align:center;\">"
                + "<td><span class=\"chk\"></span>Excellent</td>"
                + "<td><span class=\"chk\"></span>Good</td>"
                + "<td><span class=\"chk\"></span>Average</td>"
                + "<td><span class=\"chk\"></span>Poor</td>"
                + "<td><span class=\"chk\"></span>Cultural</td>"
                + "<td><span class=\"chk\"></span>Drawing</td>"
                + "<td><span class=\"chk\"></span>Amal Akhlak</td>"
                + "<td><span class=\"chk\"></span>Competition</td>"
                + "</tr>"
                + "<tr>"
                + "<td style=\"vertical-align:top;color:#6B7280;width:22mm;\">Comments:</td>"
                + "<td colspan=\"7\">" + comment + "</td>"
                + "</tr>"
                + "</table>";
    }

    private String buildGradingTableHtml(List<Grade> grades) {
        StringBuilder rows = new StringBuilder();
        for (Grade g : grades) {
            rows.append("<tr><td>").append(g.getMinMark().intValue()).append("–").append(g.getMaxMark().intValue()).append("</td>")
                    .append("<td>").append(g.getName()).append("</td>")
                    .append("<td>").append(fmtGpa(g.getGpaValue())).append("</td></tr>");
        }
        return "<table class=\"grading\">"
                + "<thead><tr class=\"th-tan\"><th>Range</th><th>Gr</th><th>GPA</th></tr></thead>"
                + "<tbody>" + rows + "</tbody>"
                + "</table>";
    }

    private String buildPhotoTag(String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            String b64 = idCardPdfService.fetchAndCompressToBase64(imageUrl);
            if (!b64.isEmpty()) {
                return "<img src=\"" + b64 + "\" style=\"width:100%;height:100%;object-fit:cover;\">";
            }
        }
        return "Student<br>Photo";
    }

    private List<Grade> loadGradingTable(Integer classId) {
        List<GradingPolicy> active = gradingPolicyRepository.findByIsActive(true);
        if (active.isEmpty()) return Collections.emptyList();
        List<Grade> grades = new ArrayList<>(active.get(0).getGrades());
        grades.sort(Comparator.comparingDouble(Grade::getMinMark).reversed());
        return grades;
    }

    private String resolveBase64(String url) {
        if (url == null || url.isBlank()) return "";
        try {
            String filename = url.substring(url.lastIndexOf("/images/") + "/images/".length());
            java.nio.file.Path path = java.nio.file.Paths.get("/var/www/student-service-images/" + filename);
            byte[] bytes = java.nio.file.Files.exists(path)
                    ? java.nio.file.Files.readAllBytes(path)
                    : new java.net.URL(url).openStream().readAllBytes();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            System.err.println("Warning: Could not resolve image " + url + ": " + e.getMessage());
            return "";
        }
    }

    private String infoRow(String label, String value, String lblClass, String valClass) {
        return "<tr><td class=\"" + lblClass + "\" style=\"width:38mm;white-space:nowrap;\">" + label + "</td>"
                + "<td class=\"" + valClass + "\" style=\"white-space:nowrap;overflow:hidden;\">: " + value + "</td></tr>";
    }

    private String resultSummaryRow(String label, String value, String lblClass, String valClass) {
        return "<tr><td class=\"" + lblClass + "\" style=\"font-weight:600;width:50%;white-space:nowrap;\">" + label + "</td>"
                + "<td class=\"" + valClass + "\" style=\"text-align:center;white-space:nowrap;\">" + value + "</td></tr>";
    }

    private String joinNonBlank(String sep, String... parts) {
        return Arrays.stream(parts)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(sep));
    }

    private String nvl(String s, String fallback) {
        return (s != null && !s.isBlank()) ? s : fallback;
    }

    private String fmtMark(BigDecimal v) {
        if (v == null) return "—";
        return v.stripTrailingZeros().toPlainString();
    }

    private String fmtGpa(Double v) {
        if (v == null) return "—";
        return String.format("%.2f", v);
    }

    private String getCss() {
        return "@page { size: A4; margin: 0; }"
                + "* { box-sizing: border-box; margin: 0; padding: 0; }"
                + "html, body { width: 210mm; background: #FFF; }"
                + "body { font-family: 'Noto Sans', Arial, sans-serif; font-size: 9pt; color: #050505; line-height: 1.35; }"
                + ".page { width: 210mm; height: 297mm; padding: 4mm; border: 2.5px solid #4A90D9; background: white; page-break-after: always; overflow: hidden; }"
                + ".page:last-child { page-break-after: auto; }"
                + ".inner { border: 2.5px solid #E0B84A; padding: 3mm 6mm; height: 100%; display: flex; flex-direction: column; }"
                + ".content-area { flex: 1; overflow: hidden; }"
                + ".bottom-area { flex-shrink: 0; }"
                + "table { border-collapse: collapse; width: 100%; }"
                + "td, th { border: 0.8px solid #6B7280; padding: 3px 5px; }"
                + ".header { text-align: center; padding-bottom: 3mm; }"
                + ".bismillah { font-size: 17pt; color: #013E5B; direction: rtl; font-family: 'Amiri','Noto Naskh Arabic',serif; }"
                + ".madrasah-name { font-size: 16pt; font-weight: 600; color: #013E5B; letter-spacing: 0.4px; }"
                + ".address { font-size: 10pt; margin-top: 1mm; }"
                + ".id-row { display: grid; grid-template-columns: 30mm 1fr 30mm; gap: 3mm; align-items: center; margin-bottom: 3mm; }"
                + ".photo { width: 26mm; height: 32mm; border: 0.8px solid #6B7280; background: #F3F4F6; display: flex; align-items: center; justify-content: center; color: #6B7280; font-size: 8pt; text-align: center; overflow: hidden; }"
                + ".logo-stack { text-align: center; }"
                + ".logo { width: 18mm; height: 18mm; border-radius: 50%; border: 1.5px solid #0F6E56; margin: 0 auto; display: flex; align-items: center; justify-content: center; color: #0F6E56; font-weight: 600; font-size: 11pt; }"
                + ".logo-wrap { display: flex; align-items: center; justify-content: center; margin: 0 auto 2mm; }"
                + ".stamp { display: inline-block; margin-top: 2mm; color: #C2185B; font-weight: 600; font-size: 11pt; letter-spacing: 0.6px; border-bottom: 1.5px solid #0F6E56; padding-bottom: 1px; }"
                + ".stamp-sub { font-size: 8.5pt; color: #013E5B; font-weight: 600; margin-top: 1mm; }"
                + ".grading { font-size: 7pt; justify-self: end; }"
                + ".grading th, .grading td { padding: 1px 3px; text-align: center; }"
                + ".th-tan { background: #FAF3E0; color: #5C3A00; font-weight: 600; }"
                + ".info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 5mm; margin-top: 2mm; font-size: 9.5pt; }"
                + ".info-grid table { width: 100%; }"
                + ".info-grid td { border: 0; padding: 1px 4px; }"
                + ".lbl-d { color: #050505; } .lbl-g { color: #0F6E56; } .lbl-r { color: #C2185B; } .lbl-p { color: #6366F1; } .lbl-b { color: #013E5B; }"
                + ".v-blue { color: #013E5B; font-weight: 600; } .v-red { color: #C2185B; font-weight: 600; }"
                + ".marks { margin-top: 3mm; font-size: 8.5pt; table-layout: fixed; width: 100%; }"
                + ".marks thead th { padding: 4px; }"
                + ".marks tbody td { padding: 2px 4px; text-align: center; }"
                + ".marks tbody td.subject { text-align: left; }"
                + ".marks tbody td.merged-grade { vertical-align: middle; font-weight: 600; background: #FAF3E0; }"
                + ".marks tfoot td { padding: 4px; font-weight: 600; }"
                + ".result-row { display: grid; grid-template-columns: 1fr 1fr 1fr; font-size: 7.5pt; margin-top: 2mm; }"
                + ".checks { margin-top: 2mm; font-size: 9pt; }"
                + ".chk { display: inline-block; width: 9px; height: 9px; border: 0.8px solid #6B7280; vertical-align: middle; margin-right: 4px; }"
                + ".chk-on { background: #013E5B; }"
                + ".signatures { display: grid; grid-template-columns: 1fr 1fr; gap: 16mm; padding-top: 8mm; font-size: 9pt; text-align: center; align-items: end; }"
                + ".sig-line { border-top: 0.8px dashed #6B7280; padding-top: 1mm; width: 60%; margin: 0 auto; }"
                + ".footer { display: flex; justify-content: space-between; margin-top: 4mm; font-size: 8pt; color: #6B7280; }";
    }
}
