package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "staff_dependent",
        indexes = @Index(name = "idx_staff_dependent_staff", columnList = "staff_id"))
@Getter
@Setter
public class StaffDependent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(name = "name", nullable = false)
    private String name;

    // e.g. SPOUSE, SON, DAUGHTER, PARENT
    @Column(name = "relationship", nullable = false)
    private String relationship;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "nid_number")
    private String nidNumber;
}
