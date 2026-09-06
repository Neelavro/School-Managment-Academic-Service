package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "result_publication",
        uniqueConstraints = @UniqueConstraint(name = "uk_result_pub_routine_class",
                columnNames = {"routine_id", "class_id"}))
@Getter
@Setter
public class ResultPublication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "routine_id", nullable = false)
    private ExamRoutine examRoutine;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id", nullable = false)
    private Class studentClass;

    @Column(nullable = false)
    private Boolean published = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
