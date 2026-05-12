package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "leave_approval_tier",
        indexes = @Index(name = "idx_leave_tier_type", columnList = "leave_type_id"))
@Getter
@Setter
public class LeaveApprovalTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "tier_order", nullable = false)
    private Integer tierOrder;

    @Column(name = "tier_label", nullable = false)
    private String tierLabel;

    // which FBAC role can approve this tier (null = any admin)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fbac_role_id")
    private FbacRole fbacRole;
}
