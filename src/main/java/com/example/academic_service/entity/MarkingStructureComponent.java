package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "marking_structure_component",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"marking_structure_id", "exam_component_id"}
        )
)
@Getter
@Setter
public class MarkingStructureComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "marking_structure_id", nullable = false)
    private MarkingStructure markingStructure;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_component_id", nullable = false)
    private ExamComponent examComponent;

    @Column(name = "max_marks", nullable = false)
    private Integer maxMarks; // must not exceed markingStructure.totalMarks (validated in service)

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