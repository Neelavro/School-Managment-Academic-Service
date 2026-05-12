package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.entity.UserType;
import com.example.academic_service.service.SystemUserService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system-users")
@RequiredArgsConstructor
public class SystemUserController {

    private final SystemUserService systemUserService;

    @GetMapping
    @RequirePermission(submodule = Submodule.USER_MANAGEMENT, action = "READ")
    public ResponseEntity<ApiResponse> getAll() {
        return ResponseEntity.ok(new ApiResponse("OK", systemUserService.getAll()));
    }

    @GetMapping("/{id}")
    @RequirePermission(submodule = Submodule.USER_MANAGEMENT, action = "READ")
    public ResponseEntity<ApiResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse("OK", systemUserService.getById(id)));
    }

    @PostMapping("/grant-access")
    @RequirePermission(submodule = Submodule.USER_MANAGEMENT, action = "CREATE")
    public ResponseEntity<ApiResponse> grantAccess(@RequestBody Map<String, Object> body) {
        Long staffId = ((Number) body.get("staffId")).longValue();
        String phone = (String) body.get("phone");
        String password = (String) body.get("password");
        UserType userType = UserType.valueOf((String) body.get("userType"));
        @SuppressWarnings("unchecked")
        List<Integer> roleIds = (List<Integer>) body.get("fbacRoleIds");
        return ResponseEntity.ok(new ApiResponse("Access granted",
                systemUserService.grantAccess(staffId, phone, password, userType, roleIds)));
    }

    @PutMapping("/{id}/roles")
    @RequirePermission(submodule = Submodule.USER_MANAGEMENT, action = "UPDATE")
    public ResponseEntity<ApiResponse> updateRoles(@PathVariable Long id,
                                                    @RequestBody Map<String, List<Integer>> body) {
        return ResponseEntity.ok(new ApiResponse("Roles updated",
                systemUserService.updateRoles(id, body.get("fbacRoleIds"))));
    }

    @PatchMapping("/{id}/suspend")
    @RequirePermission(submodule = Submodule.USER_MANAGEMENT, action = "UPDATE")
    public ResponseEntity<ApiResponse> suspend(@PathVariable Long id) {
        systemUserService.suspend(id);
        return ResponseEntity.ok(new ApiResponse("Suspended", null));
    }

    @PatchMapping("/{id}/reinstate")
    @RequirePermission(submodule = Submodule.USER_MANAGEMENT, action = "UPDATE")
    public ResponseEntity<ApiResponse> reinstate(@PathVariable Long id) {
        systemUserService.reinstate(id);
        return ResponseEntity.ok(new ApiResponse("Reinstated", null));
    }

    @PatchMapping("/{id}/force-reset")
    @RequirePermission(submodule = Submodule.USER_MANAGEMENT, action = "UPDATE")
    public ResponseEntity<ApiResponse> forceReset(@PathVariable Long id) {
        systemUserService.forcePasswordReset(id);
        return ResponseEntity.ok(new ApiResponse("Password reset forced", null));
    }

    @GetMapping("/{id}/permissions")
    @RequirePermission(submodule = Submodule.USER_MANAGEMENT, action = "READ")
    public ResponseEntity<ApiResponse> getPermissions(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse("OK", systemUserService.getPermissions(id)));
    }
}
