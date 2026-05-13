package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "designation_approval_chain",
        indexes = @Index(name = "idx_dac_designation", columnList = "designation_id"))
@Getter @Setter
public class DesignationApprovalChain {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "designation_id", nullable = false)
    private Designation designation;

    @Column(name = "tier_order", nullable = false)
    private Integer tierOrder;

    @Column(name = "tier_label", nullable = false)
    private String tierLabel;

    // null = any admin can approve
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "approver_role_id")
    private FbacRole approverRole;
}
