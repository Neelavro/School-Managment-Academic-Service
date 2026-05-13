package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.DesignationApprovalChainService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/designation-approval-chains")
@RequiredArgsConstructor
public class DesignationApprovalChainController {

    private final DesignationApprovalChainService chainService;

    @GetMapping("/{designationId}")
    @RequirePermission(submodule = Submodule.HR_DESIGNATIONS, action = "READ")
    public ResponseEntity<ApiResponse> getChain(@PathVariable Integer designationId) {
        return ResponseEntity.ok(new ApiResponse("OK", chainService.getChain(designationId)));
    }

    @PostMapping("/{designationId}/tiers")
    @RequirePermission(submodule = Submodule.HR_DESIGNATIONS, action = "UPDATE")
    public ResponseEntity<ApiResponse> addTier(@PathVariable Integer designationId,
                                                @RequestBody Map<String, Object> body) {
        String label = (String) body.get("tierLabel");
        Integer roleId = body.get("approverRoleId") != null ? ((Number) body.get("approverRoleId")).intValue() : null;
        return ResponseEntity.ok(new ApiResponse("Added", chainService.addTier(designationId, label, roleId)));
    }

    @PutMapping("/{designationId}")
    @RequirePermission(submodule = Submodule.HR_DESIGNATIONS, action = "UPDATE")
    public ResponseEntity<ApiResponse> replaceChain(@PathVariable Integer designationId,
                                                     @RequestBody List<Map<String, Object>> tiers) {
        return ResponseEntity.ok(new ApiResponse("Updated", chainService.replaceChain(designationId, tiers)));
    }

    @DeleteMapping("/{designationId}")
    @RequirePermission(submodule = Submodule.HR_DESIGNATIONS, action = "UPDATE")
    public ResponseEntity<ApiResponse> deleteChain(@PathVariable Integer designationId) {
        chainService.deleteChain(designationId);
        return ResponseEntity.ok(new ApiResponse("Deleted", null));
    }

    @DeleteMapping("/tiers/{tierId}")
    @RequirePermission(submodule = Submodule.HR_DESIGNATIONS, action = "UPDATE")
    public ResponseEntity<ApiResponse> deleteTier(@PathVariable Integer tierId) {
        chainService.deleteTier(tierId);
        return ResponseEntity.ok(new ApiResponse("Deleted", null));
    }
}
