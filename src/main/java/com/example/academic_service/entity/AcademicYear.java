package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "academic_year")
@Getter
@Setter
public class AcademicYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "year_name")
    private String yearName;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
