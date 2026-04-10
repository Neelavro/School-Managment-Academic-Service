package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_seat_allocation")
@Getter
@Setter
public class ExamSeatAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_session_id", nullable = false)
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "section_id", nullable = true)
    @NotFound(action = NotFoundAction.IGNORE)
    private Section section;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gender_section_id", nullable = true)
    @NotFound(action = NotFoundAction.IGNORE)
    private GenderSection genderSection;

    @Column(name = "room_id", nullable = true)
    private Integer roomId;

    @Column(name = "start_roll", nullable = true)
    private Integer startRoll;

    @Column(name = "end_roll", nullable = true)
    private Integer endRoll;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt = LocalDateTime.now();
}