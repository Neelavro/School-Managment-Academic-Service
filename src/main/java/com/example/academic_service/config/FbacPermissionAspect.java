package com.example.academic_service.config;

import com.example.academic_service.entity.FbacPermission;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.entity.UserType;
import com.example.academic_service.repository.SystemUserRepository;
import com.example.academic_service.repository.SystemUserRoleRepository;
import com.example.academic_service.repository.FbacPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class FbacPermissionAspect {

    private final SystemUserRepository systemUserRepository;
    private final SystemUserRoleRepository systemUserRoleRepository;
    private final FbacPermissionRepository fbacPermissionRepository;

    @Around("@annotation(requirePermission)")
    public Object check(ProceedingJoinPoint pjp, RequirePermission requirePermission) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Not authenticated");

        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        Long userId = details != null ? ((Number) details.get("userId")).longValue() : null;
        String userType = details != null ? (String) details.get("userType") : null;

        if (UserType.SUPER_ADMIN.name().equals(userType)) return pjp.proceed();

        if (userId == null) throw new AccessDeniedException("Insufficient permissions");

        List<Integer> roleIds = systemUserRoleRepository.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) throw new AccessDeniedException("No roles assigned");

        Submodule submodule = requirePermission.submodule();
        String action = requirePermission.action();

        List<FbacPermission> perms = fbacPermissionRepository.findByRoleIdsAndSubmodule(roleIds, submodule);
        boolean permitted = perms.stream().anyMatch(p -> switch (action) {
            case "CREATE" -> Boolean.TRUE.equals(p.getCanCreate());
            case "READ"   -> Boolean.TRUE.equals(p.getCanRead());
            case "UPDATE" -> Boolean.TRUE.equals(p.getCanUpdate());
            case "DELETE" -> Boolean.TRUE.equals(p.getCanDelete());
            default -> false;
        });

        if (!permitted) throw new AccessDeniedException("Permission denied: " + action + " on " + submodule);
        return pjp.proceed();
    }
}
