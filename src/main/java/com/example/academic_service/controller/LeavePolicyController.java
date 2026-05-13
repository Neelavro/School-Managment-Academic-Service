package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.LeavePolicyService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/leave-policies")
@RequiredArgsConstructor
public class LeavePolicyController {

    private final LeavePolicyService policyService;

    @GetMapping
    @RequirePermission(submodule = Submodule.HR_LEAVE_POLICIES, action = "READ")
    public ResponseEntity<ApiResponse> getAll() {
        return ResponseEntity.ok(new ApiResponse("OK", policyService.getAll()));
    }

    @GetMapping("/designation/{designationId}")
    @RequirePermission(submodule = Submodule.HR_LEAVE_POLICIES, action = "READ")
    public ResponseEntity<ApiResponse> getByDesignation(@PathVariable Integer designationId) {
        return ResponseEntity.ok(new ApiResponse("OK", policyService.getByDesignation(designationId)));
    }

    @PostMapping
    @RequirePermission(submodule = Submodule.HR_LEAVE_POLICIES, action = "CREATE")
    public ResponseEntity<ApiResponse> save(@RequestBody Map<String, Object> body) {
        Integer designationId = ((Number) body.get("designationId")).intValue();
        Integer leaveTypeId = ((Number) body.get("leaveTypeId")).intValue();
        int annualDays = ((Number) body.get("annualDays")).intValue();
        return ResponseEntity.ok(new ApiResponse("Saved", policyService.save(designationId, leaveTypeId, annualDays)));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(submodule = Submodule.HR_LEAVE_POLICIES, action = "DELETE")
    public ResponseEntity<ApiResponse> delete(@PathVariable Integer id) {
        policyService.delete(id);
        return ResponseEntity.ok(new ApiResponse("Deleted", null));
    }

    @PostMapping("/initialize/{year}")
    @RequirePermission(submodule = Submodule.HR_LEAVE_POLICIES, action = "CREATE")
    public ResponseEntity<ApiResponse> initializeYear(@PathVariable Integer year) {
        return ResponseEntity.ok(new ApiResponse("Initialized", policyService.initializeBalances(year)));
    }

    @PostMapping("/initialize/staff/{staffId}/{year}")
    @RequirePermission(submodule = Submodule.HR_LEAVE_POLICIES, action = "CREATE")
    public ResponseEntity<ApiResponse> initializeForStaff(@PathVariable Long staffId, @PathVariable Integer year) {
        policyService.initializeForStaff(staffId, year);
        return ResponseEntity.ok(new ApiResponse("Initialized", null));
    }
}
