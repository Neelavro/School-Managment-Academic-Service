package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "staff_duty_role")
@Getter
@Setter
public class StaffDutyRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "role_name", nullable = false, unique = true)
    private String roleName;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
