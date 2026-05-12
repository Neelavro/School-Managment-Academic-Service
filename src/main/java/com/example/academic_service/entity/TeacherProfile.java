package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "teacher_profile")
@Getter
@Setter
public class TeacherProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false, unique = true)
    private Staff staff;

    @Enumerated(EnumType.STRING)
    @Column(name = "mpo_status")
    private MpoStatus mpoStatus;

    @Column(name = "mpo_index_number")
    private String mpoIndexNumber;

    @Column(name = "ntrca_reg_number")
    private String ntrcaRegNumber;

    @Column(name = "ntrca_cycle")
    private String ntrcaCycle;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    // Stored as free text — e.g. "DPS STS School 2015-2018, Ideal School 2018-2022"
    @Column(name = "previous_institutions", columnDefinition = "TEXT")
    private String previousInstitutions;

    // Stored as free text — e.g. "BMTTI 2019, NAEM 2021, TQI-SEP 2023"
    @Column(name = "trainings_completed", columnDefinition = "TEXT")
    private String trainingsCompleted;
}
