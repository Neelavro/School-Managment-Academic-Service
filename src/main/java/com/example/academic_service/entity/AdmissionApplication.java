package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * A form submitted on the public online-admission page. Class, shift, group and
 * year are kept as plain ids (the list resolves their names) so an application
 * never blocks editing or deleting the class setup.
 */
@Entity
@Table(name = "admission_application")
@Getter
@Setter
public class AdmissionApplication {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    /** Times are stored as Dhaka wall-clock time whatever the server's zone (the container runs in UTC). */
    public static final ZoneId DHAKA = ZoneId.of("Asia/Dhaka");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_no", unique = true)
    private String applicationNo;

    @Column(name = "academic_year_id")
    private Integer academicYearId;

    @Column(name = "class_id", nullable = false)
    private Integer classId;

    @Column(name = "shift_id")
    private Integer shiftId;

    @Column(name = "student_group_id")
    private Integer studentGroupId;

    @Column(nullable = false)
    private String category;

    // Student
    @Column(name = "applicant_name", nullable = false)
    private String applicantName;

    @Column(nullable = false)
    private String gender;

    @Column(nullable = false)
    private String religion;

    @Column(nullable = false)
    private LocalDate dob;

    @Column(name = "blood_group")
    private String bloodGroup;

    @Column(nullable = false)
    private String nationality;

    @Column(name = "birth_certificate_no")
    private String birthCertificateNo;

    private String quota;

    @Column(name = "photo_url")
    private String photoUrl;

    // Father
    @Column(name = "father_name", nullable = false)
    private String fatherName;

    @Column(name = "father_mobile", nullable = false)
    private String fatherMobile;

    @Column(name = "father_nid", nullable = false)
    private String fatherNid;

    @Column(name = "father_occupation")
    private String fatherOccupation;

    @Column(name = "father_education")
    private String fatherEducation;

    @Column(name = "father_income")
    private String fatherIncome;

    // Mother
    @Column(name = "mother_name", nullable = false)
    private String motherName;

    @Column(name = "mother_mobile", nullable = false)
    private String motherMobile;

    @Column(name = "mother_nid", nullable = false)
    private String motherNid;

    @Column(name = "mother_occupation")
    private String motherOccupation;

    @Column(name = "mother_education")
    private String motherEducation;

    @Column(name = "mother_income")
    private String motherIncome;

    // Address
    @Column(name = "present_address", nullable = false)
    private String presentAddress;

    @Column(name = "permanent_address", nullable = false)
    private String permanentAddress;

    // Local guardian
    @Column(name = "guardian_type", nullable = false)
    private String guardianType;

    @Column(name = "guardian_name", nullable = false)
    private String guardianName;

    @Column(name = "guardian_relation", nullable = false)
    private String guardianRelation;

    @Column(name = "guardian_mobile", nullable = false)
    private String guardianMobile;

    @Column(name = "guardian_occupation")
    private String guardianOccupation;

    // Previous institute
    @Column(name = "last_institute_name")
    private String lastInstituteName;

    @Column(name = "last_class_name")
    private String lastClassName;

    @Column(name = "previous_roll")
    private String previousRoll;

    @Column(name = "previous_gpa")
    private String previousGpa;

    // Review
    @Column(nullable = false)
    private String status = PENDING;

    @Column(name = "review_note")
    private String reviewNote;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(DHAKA);
}
