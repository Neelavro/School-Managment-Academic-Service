package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "student_fourth_subject_override",
        uniqueConstraints = @UniqueConstraint(columnNames = "enrollment_id"))
@Getter
@Setter
public class StudentFourthSubjectOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enrollment_id", nullable = false, unique = true)
    private Long enrollmentId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = true)
    private Subject subject;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "student_fourth_subject_compulsory",
            joinColumns = @JoinColumn(name = "override_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    private Set<Subject> compulsorySubjects = new HashSet<>();
}
