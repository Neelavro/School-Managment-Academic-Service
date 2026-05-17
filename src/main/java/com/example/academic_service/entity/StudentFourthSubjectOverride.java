package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
}
