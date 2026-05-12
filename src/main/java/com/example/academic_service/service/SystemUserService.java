package com.example.academic_service.service;

import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    public List<SystemUser> getAll() { return userRepository.findAll(); }

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
                                   UserType userType, List<Integer> fbacRoleIds) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));

        if (userRepository.existsByPhone(phone))
            throw new IllegalArgumentException("Phone already in use: " + phone);

        SystemUser user = new SystemUser();
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setUserType(userType);
        user.setStaffId(staffId);
        user.setIsActive(true);
        user.setIsSuspended(false);
        user.setMustResetPassword(true);
        user.setCreatedAt(LocalDateTime.now());
        SystemUser saved = userRepository.save(user);

        assignRoles(saved, fbacRoleIds);
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
        if (fbacRoleIds == null) return;
        fbacRoleIds.forEach(roleId -> {
            SystemUserRole ur = new SystemUserRole();
            ur.setSystemUser(user);
            ur.setFbacRole(fbacRoleRepository.getReferenceById(roleId));
            userRoleRepository.save(ur);
        });
    }

    @Transactional
    public void assignRole(Long userId, Integer roleId) {
        SystemUser user = getById(userId);
        boolean alreadyAssigned = userRoleRepository.findBySystemUserId(userId)
                .stream().anyMatch(ur -> ur.getFbacRole().getId().equals(roleId));
        if (alreadyAssigned) return;
        SystemUserRole ur = new SystemUserRole();
        ur.setSystemUser(user);
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
    }

    public void reinstate(Long userId) {
        SystemUser u = getById(userId);
        u.setIsSuspended(false);
        userRepository.save(u);
    }

    public void forcePasswordReset(Long userId) {
        SystemUser u = getById(userId);
        u.setMustResetPassword(true);
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
