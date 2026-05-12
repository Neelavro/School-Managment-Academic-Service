package com.example.academic_service.service;

import com.example.academic_service.entity.FbacPermission;
import com.example.academic_service.entity.FbacRole;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.repository.FbacPermissionRepository;
import com.example.academic_service.repository.FbacRoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
        Arrays.stream(Submodule.values()).forEach(sub -> {
            FbacPermission p = new FbacPermission();
            p.setFbacRole(role);
            p.setSubmodule(sub);
            permissionRepository.save(p);
        });
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
        FbacRole role = roleRepository.getReferenceById(roleId);
        return permissions.stream().map(p -> {
            p.setFbacRole(role);
            return permissionRepository.save(p);
        }).collect(Collectors.toList());
    }

    // returns a map of submodule -> permission for quick lookup
    public Map<Submodule, FbacPermission> getPermissionMap(List<Integer> roleIds) {
        return Arrays.stream(Submodule.values()).collect(Collectors.toMap(
                sub -> sub,
                sub -> {
                    List<FbacPermission> perms = permissionRepository.findByRoleIdsAndSubmodule(roleIds, sub);
                    FbacPermission merged = new FbacPermission();
                    merged.setSubmodule(sub);
                    merged.setCanCreate(perms.stream().anyMatch(p -> Boolean.TRUE.equals(p.getCanCreate())));
                    merged.setCanRead(perms.stream().anyMatch(p -> Boolean.TRUE.equals(p.getCanRead())));
                    merged.setCanUpdate(perms.stream().anyMatch(p -> Boolean.TRUE.equals(p.getCanUpdate())));
                    merged.setCanDelete(perms.stream().anyMatch(p -> Boolean.TRUE.equals(p.getCanDelete())));
                    return merged;
                }
        ));
    }
}
