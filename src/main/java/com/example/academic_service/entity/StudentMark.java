package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_mark",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_sm",
                columnNames = {"enrollment_id", "routine_id", "subject_id", "exam_component_id"}
        ),
        indexes = {
                // covers: findAllByRoutineIdAndSubjectId
                // (unique constraint leads with enrollment_id, cannot serve routine+subject queries)
                @Index(name = "idx_sm_routine_subject", columnList = "routine_id, subject_id")
        }
)
@Getter
@Setter
public class StudentMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    @Column(name = "routine_id", nullable = false)
    private Integer routineId;

    @Column(name = "subject_id", nullable = false)
    private Integer subjectId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_component_id", nullable = false)
    private ExamComponent examComponent;

    @Column(name = "marks_obtained")
    private BigDecimal marksObtained;

    @Column(name = "status")
    private String status; // null / "PRESENT" / "ABSENT" / "EXPELLED"

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt = LocalDateTime.now();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}