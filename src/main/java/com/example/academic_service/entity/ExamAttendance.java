package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * How many of an exam routine's working days a student attended (typed in by hand, user 2026-11-02).
 * One row per routine + enrollment; absent days = the routine's working days minus these.
 */
@Entity
@Table(name = "exam_attendance",
        uniqueConstraints = @UniqueConstraint(name = "uq_exam_attendance", columnNames = {"exam_routine_id", "enrollment_id"}))
@Getter
@Setter
public class ExamAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_routine_id", nullable = false)
    private Integer examRoutineId;

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    @Column(name = "present_days", nullable = false)
    private Integer presentDays;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
