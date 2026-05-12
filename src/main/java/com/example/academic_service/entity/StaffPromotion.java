package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_promotion",
        indexes = @Index(name = "idx_staff_promotion_staff", columnList = "staff_id"))
@Getter
@Setter
public class StaffPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_designation_id")
    private Designation fromDesignation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_designation_id", nullable = false)
    private Designation toDesignation;

    @Column(name = "promotion_date", nullable = false)
    private LocalDate promotionDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;
}
