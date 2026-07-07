package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Short-lived per-user password reset code.
 *
 * Flow:
 *   - {@code AuthService.requestPasswordReset(phone)} generates a random
 *     6-digit code, hashes it with the same PasswordEncoder used for
 *     account passwords, and stores one row per user with a 10-minute
 *     expiry. If a row already exists for this user, it's overwritten
 *     (only the LATEST code is valid).
 *   - The raw 6-digit code is logged to stdout (until SMS/email provider
 *     is wired up), and school admins pass it to the user out-of-band.
 *   - {@code AuthService.confirmPasswordReset(phone, code, newPassword)}
 *     verifies phone → looks up the token → checks not expired → checks
 *     the raw code matches the stored hash → updates the user's
 *     password → deletes the token.
 */
@Entity
@Table(name = "password_reset_tokens",
       uniqueConstraints = @UniqueConstraint(name = "uq_prt_user", columnNames = "system_user_id"),
       indexes = {
           @Index(name = "idx_prt_expires_at", columnList = "expires_at")
       })
@Getter
@Setter
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "system_user_id", nullable = false)
    private Long systemUserId;

    /** Bcrypt hash of the raw 6-digit code — never store the code itself. */
    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
