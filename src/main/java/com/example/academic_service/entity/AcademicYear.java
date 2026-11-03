package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

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

    /** First day of the academic year (null = 1 January of the year in its name); it ends a year later minus a day. */
    @Column(name = "start_date")
    private LocalDate startDate;
}
