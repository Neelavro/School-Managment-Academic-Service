package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "staff_emergency_contact",
        indexes = @Index(name = "idx_emergency_contact_staff", columnList = "staff_id"))
@Getter
@Setter
public class StaffEmergencyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "relationship")
    private String relationship;

    @Column(name = "phone", nullable = false)
    private String phone;
}
