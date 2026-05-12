package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_approval_record",
        indexes = @Index(name = "idx_leave_approval_request", columnList = "leave_request_id"))
@Getter
@Setter
public class LeaveApprovalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_request_id", nullable = false)
    private LeaveRequest leaveRequest;

    @Column(name = "tier_order", nullable = false)
    private Integer tierOrder;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private ApprovalAction action;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "action_at")
    private LocalDateTime actionAt;
}
