package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "staff_duty_role_assignment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"staff_id", "duty_role_id"}),
        indexes = @Index(name = "idx_duty_role_assignment_staff", columnList = "staff_id"))
@Getter
@Setter
public class StaffDutyRoleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "duty_role_id", nullable = false)
    private StaffDutyRole dutyRole;
}
