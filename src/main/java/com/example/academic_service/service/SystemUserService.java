package com.example.academic_service.service;

import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import com.example.academic_service.util.AuditHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemUserService {

    private final SystemUserRepository userRepository;
    private final SystemUserRoleRepository userRoleRepository;
    private final FbacRoleRepository fbacRoleRepository;
    private final StaffRepository staffRepository;
    private final FbacRoleService fbacRoleService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    // Returns users with their assigned roles attached as a response map
    public List<Map<String, Object>> getAll() {
        List<SystemUser> users = userRepository.findAll();
        List<SystemUserRole> allRoles = userRoleRepository.findAll();

        Map<Long, List<FbacRole>> rolesByUser = allRoles.stream()
                .collect(Collectors.groupingBy(
                        ur -> ur.getSystemUser().getId(),
                        Collectors.mapping(SystemUserRole::getFbacRole, Collectors.toList())
                ));

        return users.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("phone", u.getPhone());
            m.put("userType", u.getUserType());
            m.put("staffId", u.getStaffId());
            m.put("isActive", u.getIsActive());
            m.put("isSuspended", u.getIsSuspended());
            m.put("mustResetPassword", u.getMustResetPassword());
            m.put("hasTeacherPortal", Boolean.TRUE.equals(u.getHasTeacherPortal()));
            m.put("hasAdminPortal", Boolean.TRUE.equals(u.getHasAdminPortal()));
            m.put("lastLoginAt", u.getLastLoginAt());
            m.put("createdAt", u.getCreatedAt());
            m.put("assignedRoles", rolesByUser.getOrDefault(u.getId(), Collections.emptyList())
                    .stream().map(r -> Map.of("id", r.getId(), "roleName", r.getRoleName()))
                    .collect(Collectors.toList()));
            return m;
        }).collect(Collectors.toList());
    }

    public SystemUser getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    // Bootstrap — creates first SUPER_ADMIN. Fails if one already exists.
    public SystemUser bootstrapSuperAdmin(String phone, String rawPassword) {
        if (userRepository.existsByUserType(UserType.SUPER_ADMIN))
            throw new IllegalStateException("Super admin already exists");
        SystemUser user = new SystemUser();
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setUserType(UserType.SUPER_ADMIN);
        user.setIsActive(true);
        user.setIsSuspended(false);
        user.setMustResetPassword(false);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public SystemUser grantAccess(Long staffId, String phone, String rawPassword,
                                   UserType userType, List<Integer> fbacRoleIds,
                                   boolean hasTeacherPortal, boolean hasAdminPortal) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));

        if (userRepository.existsByPhone(phone))
            throw new IllegalArgumentException("Phone already in use: " + phone);

        // Teaching staff automatically get teacher portal access
        if (EmployeeType.TEACHING == staff.getEmployeeType()) {
            hasTeacherPortal = true;
        }

        SystemUser user = new SystemUser();
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setUserType(userType);
        user.setStaffId(staffId);
        user.setIsActive(true);
        user.setIsSuspended(false);
        user.setMustResetPassword(true);
        user.setHasTeacherPortal(hasTeacherPortal);
        user.setHasAdminPortal(hasAdminPortal);
        user.setCreatedAt(LocalDateTime.now());
        SystemUser saved = userRepository.save(user);
        assignRoles(saved, fbacRoleIds);
        auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(),
            AuditActionType.CREATE, Submodule.SYSTEM_USERS, "SystemUser", saved.getId().toString(),
            "Granted system access to staffId=" + staffId + ", phone=" + phone + ", type=" + userType);
        return saved;
    }

    @Transactional
    public SystemUser updateRoles(Long userId, List<Integer> fbacRoleIds) {
        SystemUser user = getById(userId);
        userRoleRepository.deleteBySystemUserId(userId);
        assignRoles(user, fbacRoleIds);
        return user;
    }

    private void assignRoles(SystemUser user, List<Integer> fbacRoleIds) {
        if (fbacRoleIds == null || fbacRoleIds.isEmpty()) return;
        List<SystemUserRole> roles = fbacRoleIds.stream().map(roleId -> {
            SystemUserRole ur = new SystemUserRole();
            ur.setSystemUser(user);
            ur.setFbacRole(fbacRoleRepository.getReferenceById(roleId));
            return ur;
        }).collect(Collectors.toList());
        userRoleRepository.saveAll(roles);
    }

    @Transactional
    public void assignRole(Long userId, Integer roleId) {
        if (userRoleRepository.existsBySystemUserIdAndFbacRoleId(userId, roleId)) return;
        SystemUserRole ur = new SystemUserRole();
        ur.setSystemUser(userRepository.getReferenceById(userId));
        ur.setFbacRole(fbacRoleRepository.getReferenceById(roleId));
        userRoleRepository.save(ur);
    }

    @Transactional
    public void removeRole(Long userId, Integer roleId) {
        userRoleRepository.deleteBySystemUserIdAndFbacRoleId(userId, roleId);
    }

    public void suspend(Long userId) {
        SystemUser u = getById(userId);
        u.setIsSuspended(true);
        userRoleRepository.deleteBySystemUserId(userId);
        userRepository.save(u);
        auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(),
            AuditActionType.UPDATE, Submodule.SYSTEM_USERS, "SystemUser", userId.toString(),
            "Suspended system user userId=" + userId);
    }

    public void reinstate(Long userId) {
        SystemUser u = getById(userId);
        u.setIsSuspended(false);
        userRepository.save(u);
        auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(),
            AuditActionType.UPDATE, Submodule.SYSTEM_USERS, "SystemUser", userId.toString(),
            "Reinstated system user userId=" + userId);
    }

    public void forcePasswordReset(Long userId) {
        SystemUser u = getById(userId);
        u.setMustResetPassword(true);
        userRepository.save(u);
    }

    public void updatePortals(Long userId, boolean hasTeacherPortal, boolean hasAdminPortal) {
        SystemUser u = getById(userId);
        u.setHasTeacherPortal(hasTeacherPortal);
        u.setHasAdminPortal(hasAdminPortal);
        userRepository.save(u);
    }

    public void changePassword(Long userId, String rawPassword) {
        SystemUser u = getById(userId);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setMustResetPassword(false);
        userRepository.save(u);
    }

    public List<SystemUserRole> getRoles(Long userId) {
        return userRoleRepository.findBySystemUserId(userId);
    }

    // Returns the merged permission map for a user (union of all assigned roles)
    public Map<Submodule, FbacPermission> getPermissions(Long userId) {
        List<Integer> roleIds = userRoleRepository.findRoleIdsByUserId(userId);
        return fbacRoleService.getPermissionMap(roleIds);
    }
}
