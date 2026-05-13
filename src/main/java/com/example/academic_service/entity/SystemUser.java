package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_user",
        indexes = {
                @Index(name = "idx_system_user_phone", columnList = "phone"),
                @Index(name = "idx_system_user_staff", columnList = "staff_id"),
                @Index(name = "idx_system_user_type", columnList = "user_type")
        })
@Getter
@Setter
public class SystemUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone", nullable = false, unique = true)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    // links to Staff for TEACHING / ADMIN / NON_TEACHING / SUPPORT employees
    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_suspended")
    private Boolean isSuspended = false;

    @Column(name = "must_reset_password")
    private Boolean mustResetPassword = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "has_teacher_portal")
    private Boolean hasTeacherPortal = false;

    @Column(name = "has_admin_portal")
    private Boolean hasAdminPortal = false;
}
