package com.example.academic_service.controller;

import com.example.academic_service.entity.UserType;
import com.example.academic_service.service.AuthService;
import com.example.academic_service.service.PasswordResetService;
import com.example.academic_service.service.SystemUserService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SystemUserService systemUserService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody Map<String, String> body) {
        Map<String, Object> result = authService.login(body.get("phone"), body.get("password"));
        return ResponseEntity.ok(new ApiResponse("Login successful", result));
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<ApiResponse> bootstrap(@RequestBody Map<String, String> body) {
        var user = systemUserService.bootstrapSuperAdmin(body.get("phone"), body.get("password"));
        return ResponseEntity.ok(new ApiResponse("Super admin created", user));
    }

    @PostMapping("/student-login")
    public ResponseEntity<ApiResponse> studentLogin(@RequestBody Map<String, String> body) {
        Map<String, Object> result = authService.studentLogin(body.get("studentSystemId"), body.get("password"));
        return ResponseEntity.ok(new ApiResponse("Login successful", result));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        systemUserService.changePassword(userId, (String) body.get("password"));
        return ResponseEntity.ok(new ApiResponse("Password updated", null));
    }

    /**
     * Forgot-password step 1 — generates a 6-digit reset code and stores
     * its hash. The raw code is written to the academic_service stdout;
     * an admin passes it to the user (until SMS/email is wired up).
     *
     * Always returns 200 to avoid leaking whether a phone is registered.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestBody Map<String, String> body) {
        passwordResetService.requestReset(body.get("phone"));
        return ResponseEntity.ok(new ApiResponse(
                "If the phone is registered, a reset code has been generated.", null));
    }

    /**
     * Forgot-password step 2 — validates the code and updates the password.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@RequestBody Map<String, String> body) {
        passwordResetService.confirmReset(
                body.get("phone"),
                body.get("code"),
                body.get("newPassword"));
        return ResponseEntity.ok(new ApiResponse("Password updated", null));
    }
}
