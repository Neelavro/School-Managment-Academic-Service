package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "enrollment",
        indexes = {
                // covers: findAllByClassIdAndFilters — leading filter is always class_id + is_active
                @Index(name = "idx_enrollment_class_active", columnList = "class_id, is_active"),
                // covers: annual result queries that filter by year then class
                @Index(name = "idx_enrollment_year_class_active", columnList = "academic_year_id, class_id, is_active"),
                // covers: findByStudentSystemIdAndIsActive, findByStudentSystemId
                @Index(name = "idx_enrollment_sysid_active", columnList = "student_system_id, is_active")
        }
)
@Getter
@Setter
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_system_id", nullable = false)
    private String studentSystemId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_system_id", referencedColumnName = "student_system_id", insertable = false, updatable = false)
    private Student student;

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