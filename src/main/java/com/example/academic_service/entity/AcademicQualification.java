package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "academic_qualification",
        indexes = @Index(name = "idx_aq_staff", columnList = "staff_id"))
@Getter
@Setter
public class AcademicQualification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private QualificationLevel level;

    @Column(name = "institute_name")
    private String instituteName;

    @Column(name = "board")
    private String board;

    @Column(name = "group_or_subject")
    private String groupOrSubject;

    @Column(name = "passing_year")
    private Integer passingYear;

    @Column(name = "result")
    private String result;

    @Column(name = "roll_number")
    private String rollNumber;

    @Column(name = "registration_number")
    private String registrationNumber;

    // For Hifz/Qirat/Daura — sanad issuer, date, etc.
    @Column(name = "sanad_details")
    private String sanadDetails;
}
