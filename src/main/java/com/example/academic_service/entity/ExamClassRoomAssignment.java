package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "exam_class_room_assignment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"exam_routine_id", "class_id", "room_id"}))
@Getter
@Setter
public class ExamClassRoomAssignment {

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
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "start_roll", nullable = true)
    private Integer startRoll;

    @Column(name = "end_roll", nullable = true)
    private Integer endRoll;
}
