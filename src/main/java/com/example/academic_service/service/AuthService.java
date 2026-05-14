package com.example.academic_service.service;

import com.example.academic_service.config.JwtUtil;
import com.example.academic_service.entity.FbacPermission;
import com.example.academic_service.entity.Student;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.entity.SystemUser;
import com.example.academic_service.entity.UserType;
import com.example.academic_service.repository.StudentRepository;
import com.example.academic_service.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SystemUserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final SystemUserService systemUserService;
    private final StudentRepository studentRepository;

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

        List<Map<String, Object>> assignedRoles = systemUserService.getRoles(user.getId()).stream()
                .map(ur -> Map.<String, Object>of("id", ur.getFbacRole().getId(), "roleName", ur.getFbacRole().getRoleName()))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("userType", user.getUserType().name());
        response.put("mustResetPassword", Boolean.TRUE.equals(user.getMustResetPassword()));
        response.put("permissions", permissions);
        response.put("hasTeacherPortal", Boolean.TRUE.equals(user.getHasTeacherPortal()));
        response.put("hasAdminPortal", Boolean.TRUE.equals(user.getHasAdminPortal()));
        if (user.getStaffId() != null) response.put("staffId", user.getStaffId());
        response.put("assignedRoles", assignedRoles);
        return response;
    }

    public Map<String, Object> studentLogin(String studentSystemId, String password) {
        Student student = studentRepository.findByStudentSystemId(studentSystemId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!Boolean.TRUE.equals(student.getIsActive()))
            throw new IllegalStateException("Student account is inactive");

        String hash = student.getPasswordHash();
        if (hash == null || !passwordEncoder.matches(password, hash))
            throw new IllegalArgumentException("Invalid credentials");

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userType", "STUDENT");
        extraClaims.put("role", "STUDENT");
        extraClaims.put("studentSystemId", studentSystemId);

        String token = jwtUtil.generateToken(studentSystemId, extraClaims);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userType", "STUDENT");
        response.put("studentSystemId", studentSystemId);
        response.put("studentName", student.getNameEnglish());
        response.put("hasStudentPortal", true);
        return response;
    }
}
