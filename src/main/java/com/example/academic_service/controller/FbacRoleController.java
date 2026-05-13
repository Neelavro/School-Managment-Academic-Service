package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.FbacPermission;
import com.example.academic_service.entity.FbacRole;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.FbacRoleService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fbac-roles")
@RequiredArgsConstructor
public class FbacRoleController {

    private final FbacRoleService fbacRoleService;

    @GetMapping
    @RequirePermission(submodule = Submodule.ACCESS_ROLES, action = "READ")
    public ResponseEntity<ApiResponse> getAll() {
        return ResponseEntity.ok(new ApiResponse("OK", fbacRoleService.getAll()));
    }

    @GetMapping("/{id}")
    @RequirePermission(submodule = Submodule.ACCESS_ROLES, action = "READ")
    public ResponseEntity<ApiResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse("OK", fbacRoleService.getById(id)));
    }

    @PostMapping
    @RequirePermission(submodule = Submodule.ACCESS_ROLES, action = "CREATE")
    public ResponseEntity<ApiResponse> create(@RequestBody FbacRole role) {
        return ResponseEntity.ok(new ApiResponse("Created", fbacRoleService.create(role)));
    }

    @PutMapping("/{id}")
    @RequirePermission(submodule = Submodule.ACCESS_ROLES, action = "UPDATE")
    public ResponseEntity<ApiResponse> update(@PathVariable Integer id, @RequestBody FbacRole req) {
        return ResponseEntity.ok(new ApiResponse("Updated", fbacRoleService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(submodule = Submodule.ACCESS_ROLES, action = "DELETE")
    public ResponseEntity<ApiResponse> deactivate(@PathVariable Integer id) {
        fbacRoleService.deactivate(id);
        return ResponseEntity.ok(new ApiResponse("Deactivated", null));
    }

    @GetMapping("/{id}/permissions")
    @RequirePermission(submodule = Submodule.ACCESS_ROLES, action = "READ")
    public ResponseEntity<ApiResponse> getPermissions(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse("OK", fbacRoleService.getPermissions(id)));
    }

    @PutMapping("/{id}/permissions")
    @RequirePermission(submodule = Submodule.ACCESS_ROLES, action = "UPDATE")
    public ResponseEntity<ApiResponse> savePermissions(@PathVariable Integer id,
                                                        @RequestBody List<FbacPermission> permissions) {
        return ResponseEntity.ok(new ApiResponse("Saved", fbacRoleService.savePermissionMatrix(id, permissions)));
    }
}
