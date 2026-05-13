package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "leave_balance",
        uniqueConstraints = @UniqueConstraint(columnNames = {"staff_id", "leave_type_id", "year"}),
        indexes = {
            @Index(name = "idx_leave_balance_staff", columnList = "staff_id"),
            @Index(name = "idx_leave_balance_year", columnList = "year")
        })
@Getter @Setter
public class LeaveBalance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "allocated_days", nullable = false)
    private Integer allocatedDays = 0;

    @Column(name = "used_days", nullable = false)
    private Integer usedDays = 0;

    @Column(name = "pending_days", nullable = false)
    private Integer pendingDays = 0;
}
