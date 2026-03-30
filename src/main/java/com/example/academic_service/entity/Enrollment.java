package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "enrollment")
@Getter
@Setter
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_system_id", nullable = false)
    private String studentSystemId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "academic_year_id", nullable = true)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id", nullable = true)
    private Class studentClass;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "section_id", nullable = true)
    private Section section;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shift_id", nullable = true)
    private Shift shift;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gender_section_id", nullable = true)
    private GenderSection genderSection;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_group_id", nullable = true)
    private StudentGroup studentGroup;

    @Column(name = "class_roll")
    private Integer classRoll;

    @Column(name = "is_active")
    private Boolean isActive = true;
}