package com.example.academic_service.service;

import com.example.academic_service.entity.FbacPermission;
import com.example.academic_service.entity.FbacRole;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.repository.FbacPermissionRepository;
import com.example.academic_service.repository.FbacRoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FbacRoleService {

    private final FbacRoleRepository roleRepository;
    private final FbacPermissionRepository permissionRepository;

    public List<FbacRole> getAll() { return roleRepository.findAll(); }
    public List<FbacRole> getActive() { return roleRepository.findByIsActive(true); }

    public FbacRole getById(Integer id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FBAC role not found: " + id));
    }

    public FbacRole create(FbacRole role) {
        if (roleRepository.existsByRoleName(role.getRoleName()))
            throw new IllegalArgumentException("Role already exists: " + role.getRoleName());
        role.setIsActive(true);
        FbacRole saved = roleRepository.save(role);
        initPermissions(saved);
        return saved;
    }

    private void initPermissions(FbacRole role) {
        List<FbacPermission> perms = Arrays.stream(Submodule.values()).map(sub -> {
            FbacPermission p = new FbacPermission();
            p.setFbacRole(role);
            p.setSubmodule(sub);
            return p;
        }).collect(Collectors.toList());
        permissionRepository.saveAll(perms);
    }

    public FbacRole update(Integer id, FbacRole req) {
        FbacRole existing = getById(id);
        existing.setRoleName(req.getRoleName());
        existing.setDescription(req.getDescription());
        return roleRepository.save(existing);
    }

    public void deactivate(Integer id) {
        FbacRole r = getById(id);
        r.setIsActive(false);
        roleRepository.save(r);
    }

    public List<FbacPermission> getPermissions(Integer roleId) {
        return permissionRepository.findByFbacRoleId(roleId);
    }

    @Transactional
    public List<FbacPermission> savePermissionMatrix(Integer roleId, List<FbacPermission> permissions) {
        permissionRepository.deleteByFbacRoleId(roleId);
        permissionRepository.flush();
        FbacRole role = roleRepository.getReferenceById(roleId);
        permissions.forEach(p -> p.setFbacRole(role));
        return permissionRepository.saveAll(permissions);
    }

    // One query for all roleIds, merge in Java — O(n) instead of 33 round trips
    public Map<Submodule, FbacPermission> getPermissionMap(List<Integer> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return new EnumMap<>(Submodule.class);

        List<FbacPermission> all = permissionRepository.findByFbacRoleIdIn(roleIds);
        Map<Submodule, List<FbacPermission>> bySubmodule = all.stream()
                .collect(Collectors.groupingBy(FbacPermission::getSubmodule));

        Map<Submodule, FbacPermission> result = new EnumMap<>(Submodule.class);
        for (Submodule sub : Submodule.values()) {
            List<FbacPermission> perms = bySubmodule.getOrDefault(sub, Collections.emptyList());
            FbacPermission merged = new FbacPermission();
            merged.setSubmodule(sub);
            merged.setCanCreate(perms.stream().anyMatch(p -> Boolean.TRUE.equals(p.getCanCreate())));
            merged.setCanRead(perms.stream().anyMatch(p -> Boolean.TRUE.equals(p.getCanRead())));
            merged.setCanUpdate(perms.stream().anyMatch(p -> Boolean.TRUE.equals(p.getCanUpdate())));
            merged.setCanDelete(perms.stream().anyMatch(p -> Boolean.TRUE.equals(p.getCanDelete())));
            result.put(sub, merged);
        }
        return result;
    }
}
