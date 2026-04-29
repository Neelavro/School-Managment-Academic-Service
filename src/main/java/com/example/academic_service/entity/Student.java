package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "student")
@Getter
@Setter
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_system_id", unique = true)
    private String studentSystemId;

    @Column(name = "name_bangla")
    private String nameBangla;

    @Column(name = "name_english")
    private String nameEnglish;

    @Column(name = "class_roll")
    private Integer classRoll;

    // Father's Info
    @Column(name = "father_name_bangla")
    private String fatherNameBangla;

    @Column(name = "father_name_english")
    private String fatherNameEnglish;

    @Column(name = "father_occupation")
    private String fatherOccupation;

    @Column(name = "father_phone")
    private String fatherPhone;

    @Column(name = "father_monthly_salary")
    private String fatherMonthlySalary;

    // Mother's Info
    @Column(name = "mother_name_bangla")
    private String motherNameBangla;

    @Column(name = "mother_name_english")
    private String motherNameEnglish;

    @Column(name = "mother_occupation")
    private String motherOccupation;

    @Column(name = "mother_phone")
    private String motherPhone;

    @Column(name = "mother_monthly_salary")
    private String motherMonthlySalary;

    // Guardian Info
    @Column(name = "guardian_name_bangla")
    private String guardianNameBangla;

    @Column(name = "guardian_name_english")
    private String guardianNameEnglish;

    @Column(name = "guardian_occupation")
    private String guardianOccupation;

    @Column(name = "guardian_phone")
    private String guardianPhone;

    @Column(name = "guardian_relation")
    private String guardianRelation;

    // Current Address
    @Column(name = "current_holding_no")
    private String currentHoldingNo;

    @Column(name = "current_road_or_village")
    private String currentRoadOrVillage;

    @Column(name = "current_district")
    private String currentDistrict;

    @Column(name = "current_thana")
    private String currentThana;

    // Permanent Address
    @Column(name = "permanent_holding_no")
    private String permanentHoldingNo;

    @Column(name = "permanent_road_or_village")
    private String permanentRoadOrVillage;

    @Column(name = "permanent_district")
    private String permanentDistrict;

    @Column(name = "permanent_thana")
    private String permanentThana;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "nationality")
    private String nationality;

    @Column(name = "is_active")
    private Boolean isActive;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gender_id")
    private Gender gender;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_status_id")
    private StudentStatus studentStatus;

    @OneToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "image_id", nullable = true)
    private StudentImage image;
}
