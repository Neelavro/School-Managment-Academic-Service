package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A subject a teacher teaches a class in an academic year, kept apart from the class routine (LRDM, user decision
 * 2026-10-14). The teacher portal's mark entry follows these. Gender section, section and group are optional:
 * null = the whole class (or every section / group).
 */
@Entity
@Table(name = "teacher_subject",
        indexes = {
                @Index(name = "idx_ts_staff_year", columnList = "staff_id, academic_year_id"),
                @Index(name = "idx_ts_class_subject", columnList = "class_id, subject_id, academic_year_id")
        })
@Getter
@Setter
public class TeacherSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id", nullable = false)
    private Class classEntity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gender_section_id")
    private GenderSection genderSection;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "section_id")
    private Section section;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_group_id")
    private StudentGroup studentGroup;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
