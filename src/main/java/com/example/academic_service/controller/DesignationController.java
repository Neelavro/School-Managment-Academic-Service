package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.Designation;
import com.example.academic_service.entity.DesignationPromotion;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.DesignationService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/designations")
@RequiredArgsConstructor
public class DesignationController {

    private final DesignationService designationService;

    @GetMapping
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "READ")
    public ResponseEntity<ApiResponse> getAll() {
        return ResponseEntity.ok(new ApiResponse("OK", designationService.getAll()));
    }

    @PostMapping
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "CREATE")
    public ResponseEntity<ApiResponse> create(@RequestBody Designation designation) {
        return ResponseEntity.ok(new ApiResponse("Created", designationService.create(designation)));
    }

    @PutMapping("/{id}")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "UPDATE")
    public ResponseEntity<ApiResponse> update(@PathVariable Integer id, @RequestBody Designation req) {
        return ResponseEntity.ok(new ApiResponse("Updated", designationService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "DELETE")
    public ResponseEntity<ApiResponse> deactivate(@PathVariable Integer id) {
        designationService.deactivate(id);
        return ResponseEntity.ok(new ApiResponse("Deactivated", null));
    }

    // Promotion matrix
    @GetMapping("/promotion-matrix")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "READ")
    public ResponseEntity<ApiResponse> getMatrix() {
        return ResponseEntity.ok(new ApiResponse("OK", designationService.getPromotionMatrix()));
    }

    @PostMapping("/promotion-matrix")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "CREATE")
    public ResponseEntity<ApiResponse> addPath(@RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(new ApiResponse("Added",
                designationService.addPromotionPath(body.get("fromId"), body.get("toId"))));
    }

    @DeleteMapping("/promotion-matrix/{id}")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "DELETE")
    public ResponseEntity<ApiResponse> removePath(@PathVariable Integer id) {
        designationService.removePromotionPath(id);
        return ResponseEntity.ok(new ApiResponse("Removed", null));
    }
}
