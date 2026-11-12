package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


@Entity
@Table(
        name = "exam_session",
        indexes = {
                // covers: findByExamRoutineIdAndIsActiveTrue and all routine-based session loads
                @Index(name = "idx_es_routine_active", columnList = "exam_routine_id, is_active"),
                // covers: findAllByExamRoutineIdAndExamClassIdAndIsActiveTrue
                @Index(name = "idx_es_routine_class_active", columnList = "exam_routine_id, class_id, is_active")
        }
)
@Getter
@Setter
public class ExamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_routine_id", nullable = false)
    private ExamRoutine examRoutine;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id", nullable = false)
    private Class examClass;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = true)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private StudentGroup group;

    @Column(name = "start_time", nullable = true)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = true)
    private LocalTime endTime;

    @Column(name = "show_on_admit_card")
    private Boolean showOnAdmitCard = true;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt = LocalDateTime.now();

    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * Sub-subjects sat in this paper of a parent subject (user, 2026-11-12), e.g. Drawing + Singing on the same day.
     * Empty = the whole subject. Printed on the admit card as "Parent (Drawing, Singing)".
     */
    @Convert(converter = IntListConverter.class)
    @Column(name = "sub_subject_ids", length = 255)
    private java.util.List<Integer> subSubjectIds = new java.util.ArrayList<>();
}