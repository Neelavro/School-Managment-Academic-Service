package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.StaffDutyRole;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.StaffDutyRoleService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/staff-duty-roles")
@RequiredArgsConstructor
public class StaffDutyRoleController {

    private final StaffDutyRoleService dutyRoleService;

    @GetMapping
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "READ")
    public ResponseEntity<ApiResponse> getAll() {
        return ResponseEntity.ok(new ApiResponse("OK", dutyRoleService.getAll()));
    }

    @PostMapping
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "CREATE")
    public ResponseEntity<ApiResponse> create(@RequestBody StaffDutyRole role) {
        return ResponseEntity.ok(new ApiResponse("Created", dutyRoleService.create(role)));
    }

    @PutMapping("/{id}")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "UPDATE")
    public ResponseEntity<ApiResponse> update(@PathVariable Integer id, @RequestBody StaffDutyRole role) {
        return ResponseEntity.ok(new ApiResponse("Updated", dutyRoleService.update(id, role)));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "DELETE")
    public ResponseEntity<ApiResponse> deactivate(@PathVariable Integer id) {
        dutyRoleService.deactivate(id);
        return ResponseEntity.ok(new ApiResponse("Deactivated", null));
    }

    @GetMapping("/staff/{staffId}")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "READ")
    public ResponseEntity<ApiResponse> getForStaff(@PathVariable Long staffId) {
        return ResponseEntity.ok(new ApiResponse("OK", dutyRoleService.getAssignmentsForStaff(staffId)));
    }

    @PostMapping("/staff/{staffId}/assign/{dutyRoleId}")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "UPDATE")
    public ResponseEntity<ApiResponse> assign(@PathVariable Long staffId, @PathVariable Integer dutyRoleId) {
        return ResponseEntity.ok(new ApiResponse("Assigned", dutyRoleService.assign(staffId, dutyRoleId)));
    }

    @DeleteMapping("/staff/{staffId}/unassign/{dutyRoleId}")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "UPDATE")
    public ResponseEntity<ApiResponse> unassign(@PathVariable Long staffId, @PathVariable Integer dutyRoleId) {
        dutyRoleService.unassign(staffId, dutyRoleId);
        return ResponseEntity.ok(new ApiResponse("Unassigned", null));
    }
}
