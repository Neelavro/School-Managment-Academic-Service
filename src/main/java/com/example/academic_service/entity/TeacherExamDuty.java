package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "teacher_exam_duty",
        uniqueConstraints = @UniqueConstraint(columnNames = {"staff_id", "exam_session_id"}),
        indexes = {
                @Index(name = "idx_ted_staff", columnList = "staff_id"),
                @Index(name = "idx_ted_session_room", columnList = "exam_session_id, room_id")
        })
@Getter
@Setter
public class TeacherExamDuty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_session_id", nullable = false)
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
}
