package com.example.academic_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentDto {

    private Long id;
    private String studentSystemId;
    private String nameEnglish;
    private String nameBangla;
    private Integer classRoll;

    // Father
    private String fatherNameEnglish;
    private String fatherNameBangla;
    private String fatherOccupation;
    private String fatherPhone;
    private String fatherMonthlySalary;

    // Mother
    private String motherNameEnglish;
    private String motherNameBangla;
    private String motherOccupation;
    private String motherPhone;
    private String motherMonthlySalary;

    // Guardian
    private String guardianNameEnglish;
    private String guardianNameBangla;
    private String guardianOccupation;
    private String guardianPhone;
    private String guardianRelation;

    // Address
    private String currentHoldingNo;
    private String currentRoadOrVillage;
    private String currentDistrict;
    private String currentThana;
    private String permanentHoldingNo;
    private String permanentRoadOrVillage;
    private String permanentDistrict;
    private String permanentThana;

    // Other
    private LocalDate dob;
    private String nationality;
    private Boolean isActive;

    private GenderDto gender;
    private StudentStatusDto studentStatus;
    private StudentImageDto image;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GenderDto {
        private Integer id;
        private String gender;
        private Boolean isActive;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StudentStatusDto {
        private Integer id;
        private String statusName;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StudentImageDto {
        private Long id;
        private String imageUrl;
        private Boolean isActive;
    }
}