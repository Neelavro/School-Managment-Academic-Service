package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "stellar_attendance_log",
        uniqueConstraints = @UniqueConstraint(name = "uq_stellar_access_id", columnNames = "access_id"),
        indexes = {
                @Index(name = "idx_stellar_access_date", columnList = "access_date"),
                @Index(name = "idx_stellar_registration_id", columnList = "registration_id")
        }
)
@Getter
@Setter
public class StellarAttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "access_id", nullable = false)
    private Long accessId;

    @Column(name = "registration_id", length = 128)
    private String registrationId;

    @Column(name = "user_name", length = 255)
    private String userName;

    @Column(name = "unit_id", length = 64)
    private String unitId;

    @Column(name = "unit_name", length = 128)
    private String unitName;

    @Column(name = "access_date")
    private LocalDate accessDate;

    @Column(name = "access_time")
    private LocalTime accessTime;

    @Column(name = "card", length = 64)
    private String card;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt = LocalDateTime.now();
}
