package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.LeaveApprovalTier;
import com.example.academic_service.entity.LeaveType;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.LeaveTypeService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leave-types")
@RequiredArgsConstructor
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse> getAll() {
        return ResponseEntity.ok(new ApiResponse("OK", leaveTypeService.getAll()));
    }

    @PostMapping
    @RequirePermission(submodule = Submodule.HR_LEAVE_TYPES, action = "CREATE")
    public ResponseEntity<ApiResponse> create(@RequestBody LeaveType leaveType) {
        return ResponseEntity.ok(new ApiResponse("Created", leaveTypeService.create(leaveType)));
    }

    @PutMapping("/{id}")
    @RequirePermission(submodule = Submodule.HR_LEAVE_TYPES, action = "UPDATE")
    public ResponseEntity<ApiResponse> update(@PathVariable Integer id, @RequestBody LeaveType req) {
        return ResponseEntity.ok(new ApiResponse("Updated", leaveTypeService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(submodule = Submodule.HR_LEAVE_TYPES, action = "DELETE")
    public ResponseEntity<ApiResponse> deactivate(@PathVariable Integer id) {
        leaveTypeService.deactivate(id);
        return ResponseEntity.ok(new ApiResponse("Deactivated", null));
    }

    @GetMapping("/{id}/tiers")
    @RequirePermission(submodule = Submodule.HR_LEAVE_TYPES, action = "READ")
    public ResponseEntity<ApiResponse> getTiers(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse("OK", leaveTypeService.getTiers(id)));
    }

    @PostMapping("/{id}/tiers")
    @RequirePermission(submodule = Submodule.HR_LEAVE_TYPES, action = "CREATE")
    public ResponseEntity<ApiResponse> addTier(@PathVariable Integer id, @RequestBody LeaveApprovalTier tier) {
        return ResponseEntity.ok(new ApiResponse("Added", leaveTypeService.addTier(id, tier)));
    }

    @DeleteMapping("/tiers/{tierId}")
    @RequirePermission(submodule = Submodule.HR_LEAVE_TYPES, action = "DELETE")
    public ResponseEntity<ApiResponse> deleteTier(@PathVariable Integer tierId) {
        leaveTypeService.deleteTier(tierId);
        return ResponseEntity.ok(new ApiResponse("Deleted", null));
    }
}
