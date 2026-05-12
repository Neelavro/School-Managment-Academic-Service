package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "staff_class_assignment",
        indexes = {
                @Index(name = "idx_sca_staff_year", columnList = "staff_id, academic_year_id"),
                @Index(name = "idx_sca_class_year", columnList = "class_id, academic_year_id")
        })
@Getter
@Setter
public class StaffClassAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id", nullable = false)
    private Class assignedClass;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gender_section_id")
    private GenderSection genderSection;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "section_id")
    private Section section;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_group_id")
    private StudentGroup studentGroup;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
}
