package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "designation_promotion",
        uniqueConstraints = @UniqueConstraint(columnNames = {"from_designation_id", "to_designation_id"}))
@Getter
@Setter
public class DesignationPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_designation_id", nullable = false)
    private Designation fromDesignation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_designation_id", nullable = false)
    private Designation toDesignation;
}
