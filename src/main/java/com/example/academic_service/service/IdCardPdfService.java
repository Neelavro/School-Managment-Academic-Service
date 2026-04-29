package com.example.academic_service.service;

import com.example.academic_service.dto.EnrollmentResponseDto;
import com.example.academic_service.entity.SystemSettings;
import com.example.academic_service.service.SystemSettingsService;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Margin;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IdCardPdfService {

    private final SystemSettingsService systemSettingsService;

    private static final int COLS           = 6;
    private static final int ROWS           = 3;
    private static final int CARDS_PER_PAGE = COLS * ROWS;
    private static final int CARD_W         = 190;
    private static final int CARD_H         = 300;
    private static final int GAP            = 6;
    private static final int PAGE_PAD       = 10;
    private static final int PAGE_W         = COLS * CARD_W + (COLS - 1) * GAP + PAGE_PAD * 2;
    private static final int PAGE_H         = ROWS * CARD_H + (ROWS - 1) * GAP + PAGE_PAD * 2;

    private String signatureBase64;
    private String logoBase64;

    @PostConstruct
    public void init() {
        try {
            InputStream is = getClass().getResourceAsStream("/static/signature.png");
            signatureBase64 = "data:image/jpeg;base64,"
                    + Base64.getEncoder().encodeToString(is.readAllBytes());
        } catch (Exception e) {
            signatureBase64 = "";
            System.err.println("Warning: Could not load signature image. " + e.getMessage());
        }

        try {
            InputStream is = getClass().getResourceAsStream("/static/logo-2.png");
            logoBase64 = "data:image/png;base64,"
                    + Base64.getEncoder().encodeToString(is.readAllBytes());
        } catch (Exception e) {
            logoBase64 = "";
            System.err.println("Warning: Could not load logo image. " + e.getMessage());
        }
    }

    private static final String IMAGE_FOLDER = "/var/www/student-service-images/";

    private byte[] readImageBytes(String imageUrl) throws Exception {
        String filename = imageUrl.substring(imageUrl.lastIndexOf("/images/") + "/images/".length());
        java.nio.file.Path filePath = Paths.get(IMAGE_FOLDER + filename);
        if (Files.exists(filePath)) {
            return Files.readAllBytes(filePath);
        }
        return new URL(imageUrl).openStream().readAllBytes();
    }

    public String fetchAndCompressToBase64(String imageUrl) {
        int MAX_BYTES = 250 * 1024;

        try {
            byte[] rawBytes = readImageBytes(imageUrl);

            BufferedImage original = ImageIO.read(new java.io.ByteArrayInputStream(rawBytes));
            if (original == null) return "";

            try {
                com.drew.metadata.Metadata metadata = com.drew.imaging.ImageMetadataReader
                        .readMetadata(new java.io.ByteArrayInputStream(rawBytes));
                com.drew.metadata.exif.ExifIFD0Directory exif = metadata
                        .getFirstDirectoryOfType(com.drew.metadata.exif.ExifIFD0Directory.class);

                if (exif != null && exif.containsTag(com.drew.metadata.exif.ExifIFD0Directory.TAG_ORIENTATION)) {
                    int orientation = exif.getInt(com.drew.metadata.exif.ExifIFD0Directory.TAG_ORIENTATION);
                    int degrees = switch (orientation) {
                        case 3 -> 180;
                        case 6 -> 90;
                        case 8 -> 270;
                        default -> 0;
                    };

                    if (degrees != 0) {
                        boolean swap = (degrees == 90 || degrees == 270);
                        int newW = swap ? original.getHeight() : original.getWidth();
                        int newH = swap ? original.getWidth() : original.getHeight();
                        BufferedImage rotated = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
                        Graphics2D gr = rotated.createGraphics();
                        gr.translate(newW / 2.0, newH / 2.0);
                        gr.rotate(Math.toRadians(degrees));
                        gr.translate(-original.getWidth() / 2.0, -original.getHeight() / 2.0);
                        gr.drawImage(original, 0, 0, null);
                        gr.dispose();
                        original = rotated;
                    }
                }
            } catch (Exception exifEx) {
                System.err.println("Warning: Could not read EXIF, skipping rotation: " + exifEx.getMessage());
            }

            if (original.getWidth() > original.getHeight()) {
                int newW = original.getHeight();
                int newH = original.getWidth();
                BufferedImage rotated = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
                Graphics2D gr = rotated.createGraphics();
                gr.translate(newW / 2.0, newH / 2.0);
                gr.rotate(Math.toRadians(90));
                gr.translate(-original.getWidth() / 2.0, -original.getHeight() / 2.0);
                gr.drawImage(original, 0, 0, null);
                gr.dispose();
                original = rotated;
            }

            int targetW = 140;
            int targetH = (int) ((double) original.getHeight() / original.getWidth() * targetW);

            BufferedImage resized = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(original, 0, 0, targetW, targetH, null);
            g.dispose();

            float quality = 0.85f;
            byte[] result = null;

            while (quality >= 0.30f) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
                ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
                writer.setOutput(ios);
                writer.write(null, new IIOImage(resized, null, null), param);
                writer.dispose();
                ios.close();

                result = baos.toByteArray();
                if (result.length <= MAX_BYTES) break;
                quality -= 0.10f;
            }

            if (result == null || result.length == 0) return "";
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(result);

        } catch (Exception e) {
            System.err.println("Warning: Could not process student image: " + e.getMessage());
            return "";
        }
    }

    public byte[] generatePdf(List<EnrollmentResponseDto> enrollments) {
        SystemSettings settings = systemSettingsService.getSettings();
        String name    = settings.getInstitutionName() != null ? settings.getInstitutionName() : "";
        String address = settings.getAddress()         != null ? settings.getAddress()         : "";

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );
            Page page = browser.newPage();

            page.setContent(buildHtml(enrollments, name, address), new Page.SetContentOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE));

            byte[] pdf = page.pdf(new Page.PdfOptions()
                    .setPrintBackground(true)
                    .setMargin(new Margin().setTop("0").setBottom("0").setLeft("0").setRight("0"))
                    .setWidth(PAGE_W + "px")
                    .setHeight(PAGE_H + "px")
            );

            browser.close();
            return pdf;
        }
    }

    public byte[] generateBackPdf() {
        SystemSettings settings = systemSettingsService.getSettings();
        String name    = settings.getInstitutionName() != null ? settings.getInstitutionName() : "";
        String address = settings.getAddress()         != null ? settings.getAddress()         : "";
        String logo    = resolveLogoBase64(settings.getLogoUrl());

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );
            Page page = browser.newPage();

            page.setContent(buildBackHtml(name, address, logo), new Page.SetContentOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE));

            byte[] pdf = page.pdf(new Page.PdfOptions()
                    .setPrintBackground(true)
                    .setMargin(new Margin().setTop("0").setBottom("0").setLeft("0").setRight("0"))
                    .setWidth(PAGE_W + "px")
                    .setHeight(PAGE_H + "px")
            );

            browser.close();
            return pdf;
        }
    }

    private String buildHtml(List<EnrollmentResponseDto> enrollments, String institutionName, String address) {
        StringBuilder cards = new StringBuilder();

        for (int i = 0; i < enrollments.size(); i++) {
            EnrollmentResponseDto s = enrollments.get(i);

            if (i > 0 && i % CARDS_PER_PAGE == 0) {
                cards.append("<div class=\"page-break\"></div>");
            }

            String photoUrl = (s.getImage() != null && Boolean.TRUE.equals(s.getImage().getIsActive()))
                    ? fetchAndCompressToBase64(s.getImage().getImageUrl())
                    : "";

            String photoTag = photoUrl.isEmpty()
                    ? "<div class=\"photo-placeholder\">Photo</div>"
                    : "<img src=\"" + photoUrl + "\" style=\"width:100%;height:100%;object-fit:cover;\">";

            String sigTag = signatureBase64.isEmpty()
                    ? ""
                    : "<img src=\"" + signatureBase64 + "\" class=\"signature-img\">";

            String studentName = s.getNameEnglish();
            String nameFontSize = studentName.length() > 28 ? "7.5px" : studentName.length() > 22 ? "8.5px" : studentName.length() > 16 ? "10px" : "11.5px";

            StringBuilder infoRows = new StringBuilder();
            infoRows.append("<tr>")
                    .append("<td class=\"lbl\">Class</td>")
                    .append("<td class=\"sep\">:</td>")
                    .append("<td class=\"val\">").append(s.getStudentClass() != null ? s.getStudentClass().getName() : "N/A").append("</td>")
                    .append("</tr>");
            infoRows.append("<tr>")
                    .append("<td class=\"lbl\">Roll</td>")
                    .append("<td class=\"sep\">:</td>")
                    .append("<td class=\"val\">").append(s.getClassRoll() != null ? s.getClassRoll() : "N/A").append("</td>")
                    .append("</tr>");
            infoRows.append("<tr>")
                    .append("<td class=\"lbl\">Shift</td>")
                    .append("<td class=\"sep\">:</td>")
                    .append("<td class=\"val\">").append(s.getShift() != null ? s.getShift().getName() : "N/A").append("</td>")
                    .append("</tr>");
            if (s.getGenderSection() != null) {
                String sectionLabel = s.getGenderSection().getGenderName();
                if (s.getSection() != null && s.getSection().getSectionName() != null) {
                    sectionLabel += " - " + s.getSection().getSectionName();
                }
                infoRows.append("<tr>")
                        .append("<td class=\"lbl\">Section</td>")
                        .append("<td class=\"sep\">:</td>")
                        .append("<td class=\"val\">").append(sectionLabel).append("</td>")
                        .append("</tr>");
            }
            if (s.getStudentGroup() != null) {
                infoRows.append("<tr>")
                        .append("<td class=\"lbl\">Group</td>")
                        .append("<td class=\"sep\">:</td>")
                        .append("<td class=\"val\">").append(s.getStudentGroup().getName()).append("</td>")
                        .append("</tr>");
            }
            infoRows.append("<tr>")
                    .append("<td class=\"lbl\">Year</td>")
                    .append("<td class=\"sep\">:</td>")
                    .append("<td class=\"val\">").append(s.getAcademicYear() != null ? s.getAcademicYear().getYearName() : "N/A").append("</td>")
                    .append("</tr>");
            infoRows.append("<tr>")
                    .append("<td class=\"lbl\">Mobile</td>")
                    .append("<td class=\"sep\">:</td>")
                    .append("<td class=\"val\">").append(s.getMotherPhone() != null ? s.getMotherPhone() : "N/A").append("</td>")
                    .append("</tr>");

            cards.append("<div class=\"card\">")
                    .append("<svg class=\"arc-svg\" viewBox=\"0 0 190 95\" preserveAspectRatio=\"none\" height=\"95\" xmlns=\"http://www.w3.org/2000/svg\">")
                    .append("<path d=\"M0,95 L190,95 L190,24 Q95,88 0,76 Z\" fill=\"#cdeeb7\"/>")
                    .append("</svg>")
                    .append("<div class=\"card-header\"></div>")
                    .append("<div class=\"card-body\">")
                    .append("<div class=\"school-info\">")
                    .append("<div class=\"school-name\">").append(institutionName).append("</div>")
                    .append("<div class=\"school-address\">").append(address).append("</div>")
                    .append("</div>")
                    .append("<div class=\"photo-row\">")
                    .append("<div class=\"side-badge left\">ID CARD</div>")
                    .append("<div class=\"photo-frame\">").append(photoTag).append("</div>")
                    .append("<div class=\"side-badge right\">").append(s.getStudentSystemId()).append("</div>")
                    .append("</div>")
                    .append("<div class=\"student-name\" style=\"font-size:" + nameFontSize + ";\">").append(studentName).append("</div>")
                    .append("<table class=\"info-table\">").append(infoRows).append("</table>")
                    .append("<div class=\"sig-area\">")
                    .append(sigTag)
                    .append("<div class=\"sig-line\"></div>")
                    .append("<div class=\"principal-label\">PRINCIPAL</div>")
                    .append("</div>")
                    .append("</div>")
                    .append("<div class=\"card-footer\"></div>")
                    .append("</div>");
        }

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>"
                + getCss()
                + "</style></head><body>"
                + "<div class=\"grid\">" + cards + "</div>"
                + "</body></html>";
    }

    private String resolveLogoBase64(String logoUrl) {
        if (logoUrl != null && !logoUrl.isBlank()) {
            try {
                byte[] bytes = readImageBytes(logoUrl);
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
            } catch (Exception e) {
                System.err.println("Warning: Could not fetch logo, using fallback. " + e.getMessage());
            }
        }
        return logoBase64;
    }

    private String buildBackHtml(String institutionName, String address, String logoBase64Card) {
        String logoTag = logoBase64Card.isEmpty()
                ? "<div class=\"logo-placeholder\"></div>"
                : "<img src=\"" + logoBase64Card + "\" class=\"logo-img\" alt=\"Logo\">";

        String card = "<div class=\"card-back\">"
                + "<div class=\"back-top-text\">"
                + "This card is not transferable.<br>"
                + "Always carry your card with you.<br>"
                + "In case of loss, inform issuing authority as if found, please return to below address."
                + "</div>"
                + "<div class=\"back-logo-center\">" + logoTag + "</div>"
                + "<div class=\"back-school-name\">" + institutionName + "</div>"
                + "<div class=\"back-info\">"
                + "<div>" + address + "</div>"
                + "</div>"
                + "</div>";

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>"
                + getCss()
                + getBackCss()
                + "</style></head><body>"
                + "<div class=\"grid\">" + card + "</div>"
                + "</body></html>";
    }

    private String getCss() {
        return "* { box-sizing: border-box; margin: 0; padding: 0; }"
                + "body { background: #f0f0f0; font-family: 'Times New Roman', Times, serif; padding: " + PAGE_PAD + "px; }"
                + ".grid { display: flex; flex-wrap: wrap; gap: " + GAP + "px; }"
                + ".card {"
                + "  width: " + CARD_W + "px;"
                + "  height: " + CARD_H + "px;"
                + "  background: #ffffff;"
                + "  border: 2px dashed #999;"
                + "  position: relative;"
                + "  overflow: hidden;"
                + "  display: flex;"
                + "  flex-direction: column;"
                + "}"
                + ".card-header { background: #2e7d6e; height: 12px; flex-shrink: 0; }"
                + ".card-body {"
                + "  flex: 1;"
                + "  background: linear-gradient(to bottom, #b8ddf5 0%, #d4eefb 40%, #eaf6fd 65%, #ffffff 100%);"
                + "  padding: 5px 7px 0;"
                + "  position: relative;"
                + "  display: flex;"
                + "  flex-direction: column;"
                + "  justify-content: flex-start;"
                + "  align-items: stretch;"
                + "  overflow: hidden;"
                + "}"
                + ".arc-svg { position: absolute; bottom: 12px; left: 0; width: 100%; pointer-events: none; z-index: 1; }"
                + ".school-info { text-align: center; margin-bottom: 6px; position: relative; z-index: 1; flex-shrink: 0; }"
                + ".school-name { font-size: 13px; font-weight: 900; color: #1a3a8a; line-height: 1.2; white-space: normal; word-break: break-word; }"
                + ".school-address { font-size: 8px; color: #cc1a1a; font-weight: 700; margin-top: 1px; text-transform: uppercase; white-space: normal; word-break: break-word; letter-spacing: -0.5px; word-spacing: -1.5px; }"
                + ".photo-row {"
                + "  display: flex;"
                + "  align-items: flex-start;"
                + "  justify-content: space-between;"
                + "  margin-bottom: 6px;"
                + "  position: relative;"
                + "  z-index: 1;"
                + "  flex-shrink: 0;"
                + "  height: 72px;"
                + "}"
                + ".side-badge {"
                + "  background: #f5c518;"
                + "  color: #1a1a1a;"
                + "  font-size: 11px;"
                + "  font-weight: 700;"
                + "  letter-spacing: 1px;"
                + "  text-transform: uppercase;"
                + "  writing-mode: vertical-rl;"
                + "  text-orientation: mixed;"
                + "  padding: 5px 6px;"
                + "  border-radius: 3px;"
                + "  line-height: 1;"
                + "  flex-shrink: 0;"
                + "  align-self: center;"
                + "}"
                + ".side-badge.left { transform: rotate(180deg); }"
                + ".photo-frame {"
                + "  width: 62px;"
                + "  height: 72px;"
                + "  border: 2px solid #2e7d6e;"
                + "  border-radius: 3px;"
                + "  overflow: hidden;"
                + "  background: #b8d8f0;"
                + "  display: flex;"
                + "  align-items: center;"
                + "  justify-content: center;"
                + "}"
                + ".photo-placeholder { font-size: 10px; color: #4a7a9a; text-align: center; line-height: 1.4; }"
                + ".student-name {"
                + "  position: absolute;"
                + "  top: 136px;"
                + "  left: 0;"
                + "  right: 0;"
                + "  text-align: center;"
                + "  color: #5b1fa8;"
                + "  font-weight: 900;"
                + "  font-size: 12px;"
                + "  height: 14px;"
                + "  line-height: 14px;"
                + "  z-index: 2;"
                + "}"
                + ".info-table { width: 100%; border-collapse: collapse; position: relative; z-index: 1; flex-shrink: 0; margin-top: 15px; }"
                + ".info-table td { padding: 1px 2px; font-size: 8.95px; line-height: 1.3; color: #111; }"
                + ".info-table td.lbl { font-weight: 700; width: 38px; color: #111; }"
                + ".info-table td.sep { width: 8px; color: #444; }"
                + ".info-table td.val { font-weight: 600; color: #111; }"
                + ".info-table tr:not(:first-child) td { border-top: 1px dashed #cde8d0; }"
                + ".sig-area {"
                + "  display: flex;"
                + "  flex-direction: column;"
                + "  align-items: flex-end;"
                + "  position: absolute;"
                + "  bottom: 1px;"
                + "  right: 3px;"
                + "  z-index: 2;"
                + "}"
                + ".signature-img { display: block; margin-left: auto; width: 60px; height: 24px; object-fit: contain; }"
                + ".sig-line { border-top: 1px dashed #555; width: 70px; margin-bottom: 1px; }"
                + ".principal-label { font-size: 9.5px; font-weight: 700; color: #222; letter-spacing: 1px; }"
                + ".card-footer { background: #2e7d6e; height: 12px; flex-shrink: 0; }"
                + ".page-break { width: 100%; page-break-before: always; break-before: page; }";
    }

    private String getBackCss() {
        return ".card-back {"
                + "  width: " + CARD_W + "px;"
                + "  height: " + CARD_H + "px;"
                + "  background: #ffffff;"
                + "  border: 2px dashed #999;"
                + "  padding: 14px 16px;"
                + "  position: relative;"
                + "  overflow: hidden;"
                + "  display: flex;"
                + "  flex-direction: column;"
                + "  align-items: center;"
                + "  text-align: center;"
                + "}"
                + ".back-top-text { font-size: 7px; font-weight: bold; line-height: 1.5; color: #111; margin-bottom: 10px; }"
                + ".back-logo-center { margin: 6px 0; }"
                + ".logo-img { width: 50px; height: 50px; object-fit: contain; }"
                + ".logo-placeholder { font-size: 10px; font-weight: bold; color: #1d3e8a; }"
                + ".back-school-name { font-size: 9px; font-weight: bold; color: #111; line-height: 1.4; margin: 6px 0; white-space: normal; word-break: break-word; }"
                + ".back-info { font-size: 7.5px; line-height: 1.6; color: #222; margin-top: 4px; white-space: normal; word-break: break-word; }"
                + ".back-label { font-weight: bold; }"
                + ".back-footer { margin-top: 10px; font-size: 8px; font-weight: bold; color: #111; }";
    }
}
