package com.example.academic_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "grades")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grading_policy_id", nullable = false)
    private GradingPolicy gradingPolicy;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double gpaValue;

    @Column(nullable = false)
    private Double minMark;

    @Column(nullable = false)
    private Double maxMark;
}