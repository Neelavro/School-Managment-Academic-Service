package com.example.academic_service.service;

import com.example.academic_service.entity.SystemUser;
import com.example.academic_service.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Direct password reset — no OTP, no SMS, no email.
 *
 * If the phone matches an active {@link SystemUser}, we accept the new
 * password and update it. Anyone who knows a phone number can trigger a
 * reset — the school decided this trade-off is acceptable for their
 * operational context.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final SystemUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    /**
     * Look up the user by phone → return whether an active account exists
     * (so the UI can decide whether to show the "new password" form).
     * Doesn't touch the password.
     */
    public boolean userExists(String phone) {
        if (phone == null || phone.isBlank()) return false;
        return userRepo.findByPhone(phone.trim())
                .filter(u -> !Boolean.FALSE.equals(u.getIsActive()))
                .filter(u -> !Boolean.TRUE.equals(u.getIsSuspended()))
                .isPresent();
    }

    /**
     * One-shot reset: verify the phone belongs to an active user, hash
     * and store the new password, clear mustResetPassword.
     */
    @Transactional
    public void resetPassword(String phone, String newPassword) {
        if (phone == null || phone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone is required");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 6 characters");
        }
        SystemUser user = userRepo.findByPhone(phone.trim()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No account found for this phone"));
        if (Boolean.FALSE.equals(user.getIsActive())
                || Boolean.TRUE.equals(user.getIsSuspended())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Account is inactive or suspended");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustResetPassword(false);
        userRepo.save(user);
        log.info("Password reset completed for user {} ({})", user.getId(), phone);
    }
}
