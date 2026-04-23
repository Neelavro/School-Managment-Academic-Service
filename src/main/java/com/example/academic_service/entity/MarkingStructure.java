package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "marking_structure",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"exam_type_id", "class_id", "subject_id", "group_id"}
        )
)
@Getter
@Setter
public class MarkingStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_type_id", nullable = false)
    private ExamType examType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id", nullable = false)
    private Class examClass;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private StudentGroup group;

    @Column(name = "total_marks", nullable = false)
    private Integer totalMarks;

    @Column(name = "pass_marks")
    private Integer passMarks;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt = LocalDateTime.now();

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}