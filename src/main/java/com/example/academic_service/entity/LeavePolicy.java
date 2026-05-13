package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "leave_policy",
        uniqueConstraints = @UniqueConstraint(columnNames = {"designation_id", "leave_type_id"}),
        indexes = @Index(name = "idx_leave_policy_designation", columnList = "designation_id"))
@Getter @Setter
public class LeavePolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "designation_id", nullable = false)
    private Designation designation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "annual_days", nullable = false)
    private Integer annualDays;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
