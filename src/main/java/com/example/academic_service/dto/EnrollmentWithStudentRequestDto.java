package com.example.academic_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnrollmentWithStudentRequestDto {

    // ── Student fields (used to create student if not found) ──────────────────
    private String studentSystemId;   // if provided, lookup first; if null, student-service generates it
    private String nameEnglish;
    private String nameBangla;
    private String fatherNameEnglish;
    private String fatherNameBangla;
    private String fatherOccupation;
    private String fatherPhone;
    private String fatherMonthlySalary;
    private String motherNameEnglish;
    private String motherNameBangla;
    private String motherOccupation;
    private String motherPhone;
    private String motherMonthlySalary;
    private String guardianNameEnglish;
    private String guardianNameBangla;
    private String guardianOccupation;
    private String guardianPhone;
    private String guardianRelation;
    private String currentHoldingNo;
    private String currentRoadOrVillage;
    private String currentDistrict;
    private String currentThana;
    private String permanentHoldingNo;
    private String permanentRoadOrVillage;
    private String permanentDistrict;
    private String permanentThana;
    private String dob;               // "yyyy-MM-dd" string — student-service parses it
    private String nationality;
    private Integer genderId;
    private Integer studentStatusId;
    private Boolean isActive;

    // ── Enrollment fields (IDs only — service loads entities) ─────────────────
    private Integer academicYearId;
    private Integer classId;
    private Long sectionId;
    private Integer shiftId;
    private Integer genderSectionId;
    private Integer studentGroupId;
    private Integer classRoll;
}