package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "result_publication")
@Getter
@Setter
public class ResultPublication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "routine_id", nullable = false, unique = true)
    private ExamRoutine examRoutine;

    @Column(nullable = false)
    private Boolean published = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
