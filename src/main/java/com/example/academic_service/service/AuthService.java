package com.example.academic_service.service;

import com.example.academic_service.config.JwtUtil;
import com.example.academic_service.entity.FbacPermission;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.entity.SystemUser;
import com.example.academic_service.entity.UserType;
import com.example.academic_service.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SystemUserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final SystemUserService systemUserService;

    public Map<String, Object> login(String phone, String rawPassword) {
        SystemUser user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (Boolean.TRUE.equals(user.getIsSuspended()))
            throw new IllegalStateException("Account is suspended");
        if (!Boolean.TRUE.equals(user.getIsActive()))
            throw new IllegalStateException("Account is inactive");
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash()))
            throw new IllegalArgumentException("Invalid credentials");

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("userType", user.getUserType().name());
        extraClaims.put("role", user.getUserType().name());
        if (user.getStaffId() != null) extraClaims.put("staffId", user.getStaffId());

        String token = jwtUtil.generateToken(phone, extraClaims);

        Map<String, Object> permissions = new HashMap<>();
        if (user.getUserType() == UserType.SUPER_ADMIN) {
            // super admin has full access to everything
            Arrays.stream(Submodule.values()).forEach(sub -> permissions.put(sub.name(),
                    Map.of("canCreate", true, "canRead", true, "canUpdate", true, "canDelete", true)));
        } else {
            systemUserService.getPermissions(user.getId()).forEach((sub, perm) ->
                    permissions.put(sub.name(), Map.of(
                            "canCreate", Boolean.TRUE.equals(perm.getCanCreate()),
                            "canRead", Boolean.TRUE.equals(perm.getCanRead()),
                            "canUpdate", Boolean.TRUE.equals(perm.getCanUpdate()),
                            "canDelete", Boolean.TRUE.equals(perm.getCanDelete())
                    )));
        }

        return Map.of(
                "token", token,
                "userId", user.getId(),
                "userType", user.getUserType().name(),
                "mustResetPassword", Boolean.TRUE.equals(user.getMustResetPassword()),
                "permissions", permissions
        );
    }
}
