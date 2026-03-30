package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "section")
@Getter
@Setter
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_name",nullable = false)
    private String sectionName;

    @ManyToOne
    @JoinColumn(name = "class_id", nullable = false)
    private Class classEntity;

    @ManyToOne
    @JoinColumn(name = "gender_id", nullable = true)
    private GenderSection genderSection;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
