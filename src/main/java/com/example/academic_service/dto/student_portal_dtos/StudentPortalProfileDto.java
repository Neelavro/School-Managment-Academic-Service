package com.example.academic_service.dto.student_portal_dtos;

import com.example.academic_service.entity.Enrollment;
import com.example.academic_service.entity.Student;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentPortalProfileDto {

    // Identity
    private String studentSystemId;
    private String nameEnglish;
    private String nameBangla;
    private String dob;
    private String nationality;
    private String gender;
    private String studentStatus;
    private String imageUrl;

    // Father
    private String fatherNameEnglish;
    private String fatherNameBangla;
    private String fatherOccupation;
    private String fatherPhone;

    // Mother
    private String motherNameEnglish;
    private String motherNameBangla;
    private String motherOccupation;
    private String motherPhone;

    // Guardian
    private String guardianNameEnglish;
    private String guardianRelation;
    private String guardianPhone;

    // Current address
    private String currentAddress;

    // Permanent address
    private String permanentAddress;

    // Enrollment (current year)
    private String academicYear;
    private String className;
    private String genderSectionName;
    private String sectionName;
    private String studentGroupName;
    private String shiftName;
    private Integer classRoll;

    public static StudentPortalProfileDto from(Student s, Enrollment e) {
        StudentPortalProfileDto dto = new StudentPortalProfileDto();
        dto.setStudentSystemId(s.getStudentSystemId());
        dto.setNameEnglish(s.getNameEnglish());
        dto.setNameBangla(s.getNameBangla());
        dto.setDob(s.getDob() != null ? s.getDob().toString() : null);
        dto.setNationality(s.getNationality());
        if (s.getGender() != null) dto.setGender(s.getGender().getGender());
        if (s.getStudentStatus() != null) dto.setStudentStatus(s.getStudentStatus().getStatusName());
        if (s.getImage() != null) dto.setImageUrl(s.getImage().getImageUrl());

        dto.setFatherNameEnglish(s.getFatherNameEnglish());
        dto.setFatherNameBangla(s.getFatherNameBangla());
        dto.setFatherOccupation(s.getFatherOccupation());
        dto.setFatherPhone(s.getFatherPhone());

        dto.setMotherNameEnglish(s.getMotherNameEnglish());
        dto.setMotherNameBangla(s.getMotherNameBangla());
        dto.setMotherOccupation(s.getMotherOccupation());
        dto.setMotherPhone(s.getMotherPhone());

        dto.setGuardianNameEnglish(s.getGuardianNameEnglish());
        dto.setGuardianRelation(s.getGuardianRelation());
        dto.setGuardianPhone(s.getGuardianPhone());

        // Build address strings
        dto.setCurrentAddress(buildAddress(
                s.getCurrentHoldingNo(), s.getCurrentRoadOrVillage(),
                s.getCurrentThana(), s.getCurrentDistrict()));
        dto.setPermanentAddress(buildAddress(
                s.getPermanentHoldingNo(), s.getPermanentRoadOrVillage(),
                s.getPermanentThana(), s.getPermanentDistrict()));

        if (e != null) {
            if (e.getAcademicYear() != null) dto.setAcademicYear(e.getAcademicYear().getYearName());
            if (e.getStudentClass() != null) dto.setClassName(e.getStudentClass().getName());
            if (e.getGenderSection() != null) dto.setGenderSectionName(e.getGenderSection().getGenderName());
            if (e.getSection() != null) dto.setSectionName(e.getSection().getSectionName());
            if (e.getStudentGroup() != null) dto.setStudentGroupName(e.getStudentGroup().getGroupName());
            if (e.getShift() != null) dto.setShiftName(e.getShift().getName());
            dto.setClassRoll(e.getClassRoll());
        }
        return dto;
    }

    private static String buildAddress(String holding, String road, String thana, String district) {
        StringBuilder sb = new StringBuilder();
        if (holding != null && !holding.isBlank()) sb.append(holding).append(", ");
        if (road != null && !road.isBlank()) sb.append(road).append(", ");
        if (thana != null && !thana.isBlank()) sb.append(thana).append(", ");
        if (district != null && !district.isBlank()) sb.append(district);
        String result = sb.toString().trim();
        if (result.endsWith(",")) result = result.substring(0, result.length() - 1);
        return result.isBlank() ? null : result;
    }
}
