package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "teacher_period_duty",
        uniqueConstraints = @UniqueConstraint(columnNames = {"staff_id", "class_routine_id"}),
        indexes = {
                @Index(name = "idx_tpd_staff", columnList = "staff_id"),
                @Index(name = "idx_tpd_routine", columnList = "class_routine_id")
        })
@Getter
@Setter
public class TeacherPeriodDuty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_routine_id", nullable = false)
    private ClassRoutine classRoutine;
}
