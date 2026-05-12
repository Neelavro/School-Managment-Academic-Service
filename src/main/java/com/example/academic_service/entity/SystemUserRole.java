package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "system_user_role",
        uniqueConstraints = @UniqueConstraint(columnNames = {"system_user_id", "fbac_role_id"}),
        indexes = @Index(name = "idx_user_role_user", columnList = "system_user_id"))
@Getter
@Setter
public class SystemUserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_user_id", nullable = false)
    private SystemUser systemUser;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fbac_role_id", nullable = false)
    private FbacRole fbacRole;
}
