package com.example.academic_service.service;

import com.example.academic_service.dto.result_dtos.AnnualProgressReportData;
import com.example.academic_service.dto.result_dtos.AnnualResultResponse;
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

    // ─── GRAND FINAL (annual) — landscape, one column block per exam ─────────

    private static final int LAND_W = 1123;
    private static final int LAND_H = 794;

    public byte[] generateAnnual(Integer academicYearId, Integer classId, Integer shiftId,
                                 Integer genderSectionId, Long sectionId, Integer groupId,
                                 Integer startRoll, Integer endRoll) throws Exception {
        List<AnnualProgressReportData> partitions = resultService.getAnnualProgressReportDataPartitioned(
                academicYearId, classId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll);

        SystemSettings settings = systemSettingsService.getSettings();
        String html = buildAnnualHtml(partitions,
                nvl(settings.getInstitutionName(), ""), nvl(settings.getAddress(), ""), settings.getHeading(),
                resolveBase64(settings.getLogoUrl()), resolveBase64(settings.getSignatureUrl()),
                loadGradingTable(classId));

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.setContent(html, new Page.SetContentOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
            byte[] pdf = page.pdf(new Page.PdfOptions()
                    .setPrintBackground(true)
                    .setMargin(new Margin().setTop("0").setBottom("0").setLeft("0").setRight("0"))
                    .setWidth(LAND_W + "px")
                    .setHeight(LAND_H + "px"));
            browser.close();
            return pdf;
        }
    }

    String buildAnnualHtml(List<AnnualProgressReportData> partitions, String institutionName, String address,
                           String heading, String logoBase64, String signatureBase64, List<Grade> gradingTable) {
        StringBuilder pages = new StringBuilder();
        for (AnnualProgressReportData part : partitions) {
            for (AnnualResultResponse.StudentResultRow student : part.getAnnual().getStudents()) {
                pages.append(buildAnnualPage(part, student, institutionName, address, heading,
                        logoBase64, signatureBase64, gradingTable));
            }
        }
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>"
                + getCss() + getLandscapeCss()
                + "</style></head><body class=\"land\">"
                + pages
                + "</body></html>";
    }

    private String buildAnnualPage(AnnualProgressReportData data, AnnualResultResponse.StudentResultRow student,
                                   String institutionName, String address, String heading,
                                   String logoBase64, String signatureBase64, List<Grade> gradingTable) {
        AnnualResultResponse annual = data.getAnnual();
        AnnualProgressReportData.StudentDetails details = data.getDetails().get(student.getEnrollmentId());

        String photoTag = buildPhotoTag(details != null ? details.getImageUrl() : null);
        String logoTag  = logoBase64.isEmpty()
                ? "<div class=\"logo\">LOGO</div>"
                : "<img src=\"" + logoBase64 + "\" style=\"width:13mm;height:13mm;object-fit:contain;border-radius:50%;border:1.5px solid #0F6E56;\">";

        String classLabel = annual.getClassName();
        String sectionParts = joinNonBlank(" | ", student.getGenderSectionName(), student.getSectionName());
        if (!sectionParts.isEmpty()) classLabel += " — " + sectionParts;

        String motherPhone = details != null ? details.getMotherPhone() : null;
        String fatherPhone = details != null ? details.getFatherPhone() : null;
        String contactPhone = nvl(motherPhone, null) != null ? motherPhone : nvl(fatherPhone, "N/A");

        // Photo left, grading table right; school, logo and title in the middle of the page (user, 2026-10-25).
        String headerHtml = "<div class=\"gf-top\">"
                + "<div class=\"photo\">" + photoTag + "</div>"
                + "<div class=\"gf-center\">"
                + (heading != null && !heading.isBlank() ? "<div class=\"bismillah\">" + heading + "</div>" : "")
                + "<div class=\"madrasah-name\">" + institutionName + "</div>"
                + "<div class=\"address\">" + address + "</div>"
                + "<div class=\"logo-wrap\">" + logoTag + "</div>"
                + "<div class=\"stamp\">PROGRESS REPORT</div>"
                + "<div class=\"stamp-sub\">" + joinNonBlank(" - ", "GRAND FINAL", nvl(annual.getAcademicYearName(), "")) + "</div>"
                + "</div>"
                + "<div class=\"gf-grading\">" + buildGradingTableHtml(gradingTable) + "</div>"
                + "</div>";

        String infoHtml = "<div class=\"gf-info\"><table>"
                + infoRow("Name of Student", nvl(student.getStudentName(), "N/A"), "lbl-d", "v-blue")
                + infoRow("Student ID",      nvl(student.getStudentSystemId(), "N/A"), "lbl-p", "v-blue")
                + infoRow("Roll No.",        student.getClassRoll() != null ? String.valueOf(student.getClassRoll()) : "N/A", "lbl-p", "v-blue")
                + infoRow("Phone No.",       contactPhone, "lbl-g", "v-blue")
                + "</table><table>"
                + infoRow("Class",           classLabel, "lbl-b", "v-blue")
                + infoRow("Group",           nvl(student.getGroupName(), "N/A"), "lbl-r", "v-blue")
                + infoRow("Year/Session",    nvl(annual.getAcademicYearName(), "N/A"), "lbl-g", "v-blue")
                + "</table></div>";

        String marksHtml = buildAnnualMarksTable(data, student, details, gradingTable);
        String resultHtml = "<div class=\"gf-result\">" + buildAnnualSummaryTable(data, student)
                + "<table class=\"gf-comments\"><tr class=\"th-tan\"><th>Comments</th></tr>"
                + "<tr><td>" + gradeComment(student.getOverallGpa(), gradingTable) + "</td></tr></table></div>";
        String checksHtml = buildChecksHtml(student.getOverallGpa(), gradingTable, false);

        String sigHtml = "<div class=\"signatures\">"
                + "<div><div class=\"sig-line\">CLASS TEACHER</div></div>"
                + "<div>"
                + (signatureBase64.isEmpty() ? "" : "<img src=\"" + signatureBase64 + "\" style=\"height:24px;object-fit:contain;display:block;margin:0 auto;\">")
                + "<div class=\"sig-line\">PRINCIPAL</div>"
                + "</div>"
                + "</div>";

        String footerHtml = "<div class=\"footer\">"
                + "<div>Powered by Technonix</div>"
                + "<div>Date of Publication: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) + "</div>"
                + "</div>";

        return "<div class=\"page\"><div class=\"inner\">"
                + "<div class=\"content-area\">" + headerHtml + infoHtml + marksHtml + resultHtml + checksHtml + "</div>"
                + "<div class=\"bottom-area\">" + sigHtml + footerHtml + "</div>"
                + "</div></div>";
    }

    private String buildAnnualMarksTable(AnnualProgressReportData data, AnnualResultResponse.StudentResultRow student,
                                         AnnualProgressReportData.StudentDetails details, List<Grade> gradingTable) {
        List<AnnualProgressReportData.RoutineColumn> routines = data.getRoutines();
        Map<Integer, Integer> mergeGroups = data.getMergeGroups() != null ? data.getMergeGroups() : Collections.emptyMap();
        Map<String, Map<Integer, BigDecimal>> compMarks = data.getComponentMarks()
                .getOrDefault(student.getEnrollmentId(), Collections.emptyMap());
        Set<Integer> fourthBadge = details != null && details.getFourthBadgeSubjectIds() != null
                ? details.getFourthBadgeSubjectIds() : Collections.emptySet();

        Map<Integer, AnnualResultResponse.SubjectResult> resultMap = new HashMap<>();
        for (AnnualResultResponse.SubjectResult sr : student.getSubjectResults()) resultMap.put(sr.getSubjectId(), sr);

        // Rows on this card, in the annual result's subject order.
        List<AnnualResultResponse.SubjectInfo> rows = data.getAnnual().getSubjects().stream()
                .filter(si -> resultMap.containsKey(si.getSubjectId()))
                .collect(Collectors.toList());
        Map<Integer, List<AnnualResultResponse.SubjectInfo>> byMergeGroup = new LinkedHashMap<>();
        for (AnnualResultResponse.SubjectInfo si : rows) {
            AnnualResultResponse.SubjectResult sr = resultMap.get(si.getSubjectId());
            Integer mg = sr.isFourthSubject() ? null : mergeGroups.get(si.getSubjectId());
            if (mg != null) byMergeGroup.computeIfAbsent(mg, k -> new ArrayList<>()).add(si);
        }

        // Column widths: subject wider, everything else equal.
        int examCols = routines.stream().mapToInt(r -> r.getComponents().size() + 2).sum();
        int totalCols = 4 + examCols;
        double subjectPct = 15;
        double eachOther = (100 - subjectPct) / (totalCols - 1);
        StringBuilder colgroup = new StringBuilder("<colgroup>");
        colgroup.append("<col style=\"width:").append(String.format(Locale.ROOT, "%.2f", subjectPct)).append("%\">");
        for (int i = 1; i < totalCols; i++)
            colgroup.append("<col style=\"width:").append(String.format(Locale.ROOT, "%.2f", eachOther)).append("%\">");
        colgroup.append("</colgroup>");

        StringBuilder head1 = new StringBuilder("<tr class=\"th-tan\">"
                + "<th rowspan=\"2\">Name of Subjects</th>");
        StringBuilder head2 = new StringBuilder("<tr class=\"th-tan\" style=\"font-weight:400;\">");
        for (AnnualProgressReportData.RoutineColumn r : routines) {
            head1.append("<th class=\"exam-head\" colspan=\"").append(r.getComponents().size() + 2).append("\">")
                    .append(nvl(r.getTitle(), "")).append("</th>");
            for (ProgressReportData.ComponentInfo c : r.getComponents())
                head2.append("<th class=\"comp-head\">").append(c.getComponentName()).append("</th>");
            head2.append("<th class=\"comp-head exam-total\">Total<br>Marks</th>");
            head2.append("<th class=\"comp-head exam-end\">Grade</th>");
        }
        head1.append("<th rowspan=\"2\">Total<br>Marks</th>"
                + "<th rowspan=\"2\">Letter<br>Grade</th>"
                + "<th rowspan=\"2\">Grade<br>Point</th></tr>");
        head2.append("</tr>");
        String thead = "<thead>" + head1 + head2 + "</thead>";

        StringBuilder tbody = new StringBuilder("<tbody>");
        BigDecimal totalFull = BigDecimal.ZERO;
        BigDecimal totalObtained = BigDecimal.ZERO;
        Set<Integer> doneGroups = new HashSet<>();

        for (AnnualResultResponse.SubjectInfo si : rows) {
            AnnualResultResponse.SubjectResult sr = resultMap.get(si.getSubjectId());
            Integer mg = sr.isFourthSubject() ? null : mergeGroups.get(si.getSubjectId());
            List<AnnualResultResponse.SubjectInfo> block = mg != null ? byMergeGroup.get(mg) : List.of(si);
            if (mg != null) {
                if (!doneGroups.add(mg)) continue;
            }

            // Merged pairs share one grade, worked out from their combined marks.
            String blockGrade = null, blockGpa = null;
            if (mg != null) {
                BigDecimal obtained = BigDecimal.ZERO;
                int full = 0;
                boolean anyAppeared = false, anyExpelled = false;
                for (AnnualResultResponse.SubjectInfo m : block) {
                    AnnualResultResponse.SubjectResult msr = resultMap.get(m.getSubjectId());
                    if (msr.isAppeared()) {
                        anyAppeared = true;
                        if (msr.getMarksRaw() != null) obtained = obtained.add(msr.getMarksRaw());
                    }
                    if ("EXPELLED".equals(msr.getStatus())) anyExpelled = true;
                    if (msr.getMaxMarksRaw() != null) full += msr.getMaxMarksRaw();
                }
                blockGrade = !anyAppeared ? "—" : anyExpelled ? "F" : resolveMergedGrade(obtained, full, gradingTable);
                blockGpa   = !anyAppeared ? "—" : anyExpelled ? "0.00" : resolveMergedGpa(obtained, full, gradingTable);
            }

            for (int i = 0; i < block.size(); i++) {
                AnnualResultResponse.SubjectInfo row = block.get(i);
                AnnualResultResponse.SubjectResult r = resultMap.get(row.getSubjectId());
                boolean appeared = r.isAppeared() && r.getMarksRaw() != null;
                String rowClass = mg == null ? "" : i == 0 ? " class=\"merged-first\"" : " class=\"merged-last\"";

                tbody.append("<tr").append(rowClass).append(">");
                tbody.append("<td class=\"subject\">").append(row.getSubjectName())
                        .append(r.isFourthSubject() && fourthBadge.contains(row.getSubjectId()) ? " (4<sup>th</sup>)" : "")
                        .append("</td>");

                for (AnnualProgressReportData.RoutineColumn rc : routines) {
                    AnnualResultResponse.RoutineBreakdown rb = r.getRoutineBreakdowns() == null ? null
                            : r.getRoutineBreakdowns().stream()
                                .filter(b -> rc.getRoutineId().equals(b.getRoutineId())).findFirst().orElse(null);
                    int span = rc.getComponents().size() + 2;
                    if (rb != null && !rb.isAppeared() && rb.getStatus() != null) {
                        tbody.append("<td colspan=\"").append(span).append("\" class=\"status-mark exam-end\">")
                                .append("EXPELLED".equals(rb.getStatus()) ? "EXPELLED" : "ABSENT").append("</td>");
                        continue;
                    }
                    Map<Integer, BigDecimal> cm = compMarks.getOrDefault(rc.getRoutineId() + "_" + row.getSubjectId(), Collections.emptyMap());
                    boolean sat = rb != null && rb.isAppeared();
                    for (ProgressReportData.ComponentInfo c : rc.getComponents()) {
                        BigDecimal m = sat ? cm.get(c.getComponentId()) : null;
                        tbody.append("<td>").append(m != null ? fmtMark(m) : "—").append("</td>");
                    }
                    tbody.append("<td class=\"exam-total\">")
                            .append(sat ? fmtMark(rb.getMarksObtained()) : "—").append("</td>");
                    tbody.append("<td class=\"exam-end\">")
                            .append(sat && rb.getGradeName() != null ? rb.getGradeName() : "—").append("</td>");
                }

                tbody.append("<td class=\"annual-total\">").append(appeared ? fmtMark(r.getMarksRaw()) : "—").append("</td>");
                if (mg == null) {
                    tbody.append("<td>").append(appeared && r.getGradeName() != null ? r.getGradeName() : "—").append("</td>");
                    tbody.append("<td>").append(appeared && r.getGpaValue() != null ? fmtGpa(r.getGpaValue()) : "—").append("</td>");
                } else if (i == 0) {
                    tbody.append("<td rowspan=\"").append(block.size()).append("\" class=\"merged-grade\">").append(blockGrade).append("</td>");
                    tbody.append("<td rowspan=\"").append(block.size()).append("\" class=\"merged-grade\">").append(blockGpa).append("</td>");
                }
                tbody.append("</tr>");

                // Same as the per-exam card: every subject on the card counts, 4th included.
                if (r.getMaxMarksRaw() != null) totalFull = totalFull.add(BigDecimal.valueOf(r.getMaxMarksRaw()));
                if (appeared) totalObtained = totalObtained.add(r.getMarksRaw());
            }
        }
        tbody.append("</tbody>");

        String gpaStr = student.getOverallGpa() != null ? fmtGpa(student.getOverallGpa()) : "—";
        String tfoot = "<tfoot><tr>"
                + "<td colspan=\"" + (examCols + 1) + "\" style=\"color:#0F6E56;text-align:right;padding-right:3mm;\">Obtained Marks &amp; GPA</td>"
                + "<td style=\"color:#C2185B;text-align:center;\">" + fmtMark(totalObtained) + "</td>"
                + "<td style=\"color:#C2185B;text-align:center;\">" + resolveGradeNameFromGpa(student.getOverallGpa(), null) + "</td>"
                + "<td style=\"color:#C2185B;text-align:center;\">" + gpaStr + "</td>"
                + "</tr></tfoot>";

        String density = rows.size() > 12 ? " dense" : "";
        return "<table class=\"marks" + density + "\">" + colgroup + thead + tbody + tfoot + "</table>";
    }

    /** Obtained marks on the Grand Final card: every paper on it that was sat, 4th included (as the per-exam card). */
    private BigDecimal annualCardTotal(AnnualProgressReportData data, AnnualResultResponse.StudentResultRow student) {
        Set<Integer> onCard = data.getAnnual().getSubjects().stream()
                .map(AnnualResultResponse.SubjectInfo::getSubjectId).collect(Collectors.toSet());
        BigDecimal total = BigDecimal.ZERO;
        for (AnnualResultResponse.SubjectResult r : student.getSubjectResults())
            if (onCard.contains(r.getSubjectId()) && r.isAppeared() && r.getMarksRaw() != null) total = total.add(r.getMarksRaw());
        return total;
    }

    /**
     * Each exam's result and the Grand Final under the marks (like LRDM's printed transcript): total, letter grade,
     * grade point, positions, result, GPA without 4th, failed subjects, and blank attendance cells.
     */
    private String buildAnnualSummaryTable(AnnualProgressReportData data, AnnualResultResponse.StudentResultRow student) {
        StringBuilder rows = new StringBuilder();
        Map<Integer, Map<Long, ProgressReportData.StudentReport>> summaries =
                data.getExamSummaries() != null ? data.getExamSummaries() : Collections.emptyMap();
        for (AnnualProgressReportData.RoutineColumn rc : data.getRoutines()) {
            ProgressReportData.StudentReport s = summaries.getOrDefault(rc.getRoutineId(), Collections.emptyMap())
                    .get(student.getEnrollmentId());
            if (s == null) {
                rows.append(summaryRow(nvl(rc.getTitle(), ""), "—", "—", "—", "—", "—", "—", "—", "—", "—", false));
                continue;
            }
            rows.append(summaryRow(nvl(rc.getTitle(), ""), fmtMark(s.getTotalMarks()),
                    resolveGradeNameFromGpa(s.getOverallGpa(), null), fmtGpa(s.getOverallGpa()),
                    rank(s.getClassRank()), rank(s.getGenderSectionRank()), rank(s.getSectionRank()),
                    s.isPassed() ? "Passed" : "Failed", fmtGpa(s.getGpaWithout4th()),
                    String.valueOf(s.getFailedSubjectCount()), false));
        }
        AnnualResultResponse.Merit m = student.getMerit();
        rows.append(summaryRow("Grand Final", fmtMark(annualCardTotal(data, student)),
                resolveGradeNameFromGpa(student.getOverallGpa(), null), fmtGpa(student.getOverallGpa()),
                rank(m != null ? m.getClassRank() : null), rank(m != null ? m.getGenderSectionRank() : null),
                rank(m != null ? m.getSectionRank() : null),
                student.isPassed() ? "Passed" : "Failed", fmtGpa(student.getGpaWithout4th()),
                String.valueOf(student.getFailedSubjectCount()), true));
        return "<table class=\"gf-summary\"><thead><tr class=\"th-tan\">"
                + "<th class=\"exam\">Exam Name</th><th>Total Marks</th><th>Letter Grade</th><th>Grade Point</th>"
                + "<th>Class Position</th><th>Shift Position</th><th>Section Position</th><th>Result Status</th>"
                + "<th>Without 4th</th><th>Failed Subject(s)</th><th>Working Days</th><th>Total Present</th><th>Total Absent</th>"
                + "</tr></thead><tbody>" + rows + "</tbody></table>";
    }

    private String summaryRow(String exam, String total, String letter, String gpa, String classPos, String shiftPos,
                              String sectionPos, String status, String without4th, String failed, boolean grand) {
        return "<tr" + (grand ? " class=\"grand\"" : "") + ">"
                + "<td class=\"exam\">" + exam + "</td><td>" + total + "</td><td>" + letter + "</td><td>" + gpa + "</td>"
                + "<td>" + classPos + "</td><td>" + shiftPos + "</td><td>" + sectionPos + "</td>"
                + "<td class=\"" + ("Failed".equals(status) ? "failed" : "") + "\">" + status + "</td>"
                + "<td>" + without4th + "</td><td>" + failed + "</td><td></td><td></td><td></td></tr>";
    }

    private static String rank(Integer r) {
        return r != null ? String.valueOf(r) : "—";
    }

    /** Overrides on top of getCss() for the landscape Grand Final card. */
    private String getLandscapeCss() {
        return "@page { size: A4 landscape; margin: 0; }"
                + "html { width: 297mm; }"
                + "body.land { width: 297mm; }"
                + ".land .page { width: 297mm; height: 210mm; padding: 3.5mm; }"
                + ".land .inner { padding: 2.5mm 6mm; }"
                + ".land .header { padding-bottom: 2mm; }"
                + ".land .bismillah { font-size: 15pt; }"
                + ".land .madrasah-name { font-size: 15pt; }"
                + ".land .address { font-size: 9pt; margin-top: 0.5mm; }"
                + ".land .gf-top { display: grid; grid-template-columns: 1fr auto 1fr; align-items: start; gap: 4mm; }"
                + ".land .gf-top .photo { justify-self: start; }"
                + ".land .gf-center { text-align: center; }"
                + ".land .gf-grading { justify-self: end; }"
                + ".land .photo { width: 22mm; height: 26mm; }"
                + ".land .gf-info { display: grid; grid-template-columns: 1fr 1fr; gap: 6mm; align-items: start; margin-top: 1mm; font-size: 8.5pt; }"
                + ".land .gf-info table { width: auto; }"
                + ".land .gf-info td { border: 0; padding: 0 4px; }"
                + ".land .logo-wrap { margin: 1mm auto 0; }"
                + ".land .stamp { margin-top: 0.5mm; font-size: 10pt; }"
                + ".land .stamp-sub { margin-top: 0.5mm; }"
                + ".land .gf-center .logo { width: 13mm; height: 13mm; font-size: 9pt; }"
                + ".land .gf-result { display: grid; grid-template-columns: 1fr 50mm; gap: 2mm; margin-top: 1.5mm; align-items: stretch; }"
                + ".land .gf-comments { width: 100%; font-size: 7.5pt; }"
                + ".land .gf-comments td { vertical-align: top; height: 100%; }"
                + ".land .gf-summary { width: 100%; font-size: 7.5pt; table-layout: fixed; }"
                + ".land .gf-summary th { padding: 2px 2px; line-height: 1.15; }"
                + ".land .gf-summary td { padding: 1px 3px; text-align: center; }"
                + ".land .gf-summary .exam { width: 15%; text-align: left; }"
                + ".land .gf-summary tr.grand td { font-weight: 700; }"
                + ".land .gf-summary td.failed { color: #C2185B; font-weight: 600; }"
                + ".land .grading { font-size: 6.5pt; }"
                + ".land .grading th, .land .grading td { padding: 0 3px; }"
                + ".land .marks { margin-top: 1.5mm; font-size: 8pt; }"
                + ".land .marks thead th { padding: 2px 2px; line-height: 1.2; }"
                + ".land .marks thead th.exam-head { color: #013E5B; font-size: 8.5pt; }"
                + ".land .marks thead th.comp-head { font-size: 7pt; word-break: break-word; }"
                + ".land .marks tbody td { padding: 1.5px 3px; }"
                + ".land .marks td.exam-total { font-weight: 600; color: #013E5B; }"
                + ".land .marks .exam-end { border-right: 1.6px solid #6B7280; }"
                + ".land .marks td.annual-total { font-weight: 600; color: #C2185B; }"
                + ".land .marks tfoot td { padding: 3px; }"
                + ".land .marks.dense { font-size: 7.5pt; }"
                + ".land .marks.dense tbody td { padding: 0.5px 3px; }"
                + ".land .result-row { margin-top: 1.5mm; }"
                + ".land .checks { margin-top: 1.5mm; font-size: 8.5pt; }"
                + ".land .checks td, .land .checks th { padding: 2px 5px; }"
                + ".land .signatures { padding-top: 5mm; }"
                + ".land .footer { margin-top: 2mm; }";
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
                boolean anyExpelled = false;
                for (ProgressReportData.SubjectInfo msi : pair) {
                    ProgressReportData.SubjectResult msr = resultMap.get(msi.getSubjectId());
                    if (msr != null && msr.isAppeared()) {
                        anyAppeared = true;
                        if (msr.getTotalMarks() != null)
                            combinedObtained = combinedObtained.add(msr.getTotalMarks());
                    }
                    if (msr != null && "EXPELLED".equals(msr.getStatus())) anyExpelled = true;
                    if (msi.getTotalMarks() != null) combinedFull += msi.getTotalMarks();
                }
                String combinedGrade = !anyAppeared ? "—" : anyExpelled ? "F" : resolveMergedGrade(combinedObtained, combinedFull, gradingTable);
                String combinedGpa   = !anyAppeared ? "—" : anyExpelled ? "0.00" : resolveMergedGpa(combinedObtained, combinedFull, gradingTable);

                for (int i = 0; i < rowCount; i++) {
                    ProgressReportData.SubjectInfo msi = pair.get(i);
                    ProgressReportData.SubjectResult msr = resultMap.get(msi.getSubjectId());
                    boolean appeared = msr != null && msr.isAppeared();
                    String statusLabel = nonAppearedStatusLabel(msr);
                    String totalCell = appeared && msr.getTotalMarks() != null ? fmtMark(msr.getTotalMarks()) : "—";
                    String highCell  = fmtMark(msi.getHighestMarks());

                    String rowStyle = i == 0 ? " class=\"merged-first\"" : " class=\"merged-last\"";
                    tbody.append("<tr").append(rowStyle).append(">");
                    tbody.append("<td class=\"subject\">").append(msi.getSubjectName()).append("</td>");
                    tbody.append("<td>").append(msi.getTotalMarks()).append("</td>");
                    tbody.append("<td>").append(highCell).append("</td>");
                    if (statusLabel != null) {
                        int span = components.size() + 1;
                        tbody.append("<td colspan=\"").append(span).append("\" class=\"status-mark\">").append(statusLabel).append("</td>");
                    } else {
                        for (ProgressReportData.ComponentInfo c : components) {
                            BigDecimal cm = msr != null ? msr.getComponentMarks().get(c.getComponentId()) : null;
                            tbody.append("<td>").append(cm != null ? fmtMark(cm) : "—").append("</td>");
                        }
                        tbody.append("<td>").append(totalCell).append("</td>");
                    }
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
                String statusLabel = nonAppearedStatusLabel(sr);
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
                if (statusLabel != null) {
                    int span = components.size() + 1;
                    tbody.append("<td colspan=\"").append(span).append("\" class=\"status-mark\">").append(statusLabel).append("</td>");
                } else {
                    for (ProgressReportData.ComponentInfo c : components) {
                        BigDecimal cm = sr != null ? sr.getComponentMarks().get(c.getComponentId()) : null;
                        tbody.append("<td>").append(cm != null ? fmtMark(cm) : "—").append("</td>");
                    }
                    tbody.append("<td>").append(totalCell).append("</td>");
                }
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
        return buildChecksHtml(overallGpa, gradingTable, true);
    }

    /** The grade's comment for a GPA ("" when none). */
    private String gradeComment(Double overallGpa, List<Grade> gradingTable) {
        if (overallGpa == null || gradingTable.isEmpty()) return "";
        // gradingTable is sorted by minMark desc (A+ first), gpaValue also descending
        return gradingTable.stream()
                .filter(g -> g.getGpaValue() != null && overallGpa >= g.getGpaValue() - 0.001)
                .findFirst()
                .map(g -> g.getComment() != null ? g.getComment() : "")
                .orElse("");
    }

    private String buildChecksHtml(Double overallGpa, List<Grade> gradingTable, boolean withComments) {
        String comment = gradeComment(overallGpa, gradingTable);
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
                + (withComments
                    ? "<tr>"
                      + "<td style=\"vertical-align:top;color:#6B7280;width:22mm;\">Comments:</td>"
                      + "<td colspan=\"7\">" + comment + "</td>"
                      + "</tr>"
                    : "")
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

    private String nonAppearedStatusLabel(ProgressReportData.SubjectResult sr) {
        if (sr == null || sr.isAppeared()) return null;
        if ("EXPELLED".equals(sr.getStatus())) return "EXPELLED";
        return "ABSENT";
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
                + ".marks tbody td.status-mark { font-weight: 600; color: #C2185B; letter-spacing: 0.5px; }"
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
