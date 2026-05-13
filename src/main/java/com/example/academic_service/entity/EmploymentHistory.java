package com.example.academic_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employment_history")
@Getter
@Setter
public class EmploymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    @JsonIgnore
    private Staff staff;

    @Column(name = "organization_name", nullable = false)
    private String organizationName;

    @Column(name = "designation")
    private String designation;

    @Column(name = "from_date")
    private String fromDate;

    // null means currently working there
    @Column(name = "to_date")
    private String toDate;

    @Column(name = "job_type")
    private String jobType;

    @Column(name = "responsibilities", length = 1000)
    private String responsibilities;
}
