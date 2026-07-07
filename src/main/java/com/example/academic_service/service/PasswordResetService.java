package com.example.academic_service.service;

import com.example.academic_service.entity.PasswordResetToken;
import com.example.academic_service.entity.SystemUser;
import com.example.academic_service.repository.PasswordResetTokenRepository;
import com.example.academic_service.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Forgot-password flow. Generates a short-lived 6-digit code, hashes it
 * into {@code password_reset_tokens}, and logs the raw code to stdout so
 * a school admin can pass it out-of-band (until SMS/email provider is
 * wired up).
 *
 * Rate-limiting is deliberately minimal — only one active reset row per
 * user (the unique key on system_user_id enforces it), and every new
 * request overwrites the previous code.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int CODE_TTL_MINUTES = 10;
    private static final SecureRandom RNG = new SecureRandom();

    private final SystemUserRepository userRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final PasswordEncoder passwordEncoder;

    /**
     * Generate + persist a 6-digit reset code for the phone. To avoid
     * user-enumeration attacks, we DON'T tell the caller whether the
     * phone exists — the FE always gets a 200 either way, and we only
     * log/store when the phone matches a real active user.
     */
    @Transactional
    public void requestReset(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone is required");
        }
        SystemUser user = userRepo.findByPhone(phone.trim()).orElse(null);
        if (user == null) {
            log.info("Password reset requested for unknown phone {} — silently ignoring", phone);
            return;
        }
        if (Boolean.FALSE.equals(user.getIsActive())
                || Boolean.TRUE.equals(user.getIsSuspended())) {
            log.info("Password reset requested for inactive/suspended user {} — silently ignoring", phone);
            return;
        }

        String code = generateCode();
        PasswordResetToken token = tokenRepo.findBySystemUserId(user.getId())
                .orElseGet(PasswordResetToken::new);
        token.setSystemUserId(user.getId());
        token.setCodeHash(passwordEncoder.encode(code));
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES));
        tokenRepo.save(token);

        // Print the code where a school admin can see it. Replace with
        // SMS / email delivery once the provider is chosen.
        log.warn("PASSWORD RESET CODE for user {} ({}): {}  (valid {} min)",
                user.getId(), phone, code, CODE_TTL_MINUTES);
    }

    /**
     * Validate the code and update the password.
     */
    @Transactional
    public void confirmReset(String phone, String code, String newPassword) {
        if (phone == null || phone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone is required");
        }
        if (code == null || code.length() != 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter the 6-digit code");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 6 characters");
        }
        SystemUser user = userRepo.findByPhone(phone.trim()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid phone or code"));
        PasswordResetToken token = tokenRepo.findBySystemUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid phone or code"));
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenRepo.delete(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Reset code has expired. Request a new one.");
        }
        if (!passwordEncoder.matches(code, token.getCodeHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid phone or code");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustResetPassword(false);
        userRepo.save(user);
        tokenRepo.deleteBySystemUserId(user.getId());

        log.info("Password reset completed for user {} ({})", user.getId(), phone);
    }

    private String generateCode() {
        int n = RNG.nextInt(1_000_000); // 0 to 999_999
        return String.format("%06d", n);
    }
}
