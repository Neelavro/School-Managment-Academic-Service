package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.dto.FeeCategoryRequest;
import com.example.academic_service.dto.FeeCategoryResponse;
import com.example.academic_service.dto.FeePricingRow;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.FeeCategoryService;
import com.example.academic_service.service.FeePricingService;
import com.example.academic_service.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounting/fee-categories")
@RequiredArgsConstructor
public class FeeCategoryController {

    private final FeeCategoryService categoryService;
    private final FeePricingService pricingService;

    @GetMapping
    @RequirePermission(submodule = Submodule.ACCOUNTS_FEE_CATEGORIES, action = "READ")
    public ResponseEntity<ApiResponse<List<FeeCategoryResponse>>> getAll() {
        return ResponseEntity.ok(new ApiResponse<>("OK", categoryService.getAll()));
    }

    @GetMapping("/{id}")
    @RequirePermission(submodule = Submodule.ACCOUNTS_FEE_CATEGORIES, action = "READ")
    public ResponseEntity<ApiResponse<FeeCategoryResponse>> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("OK", categoryService.getOne(id)));
    }

    @PostMapping
    @RequirePermission(submodule = Submodule.ACCOUNTS_FEE_CATEGORIES, action = "CREATE")
    public ResponseEntity<ApiResponse<FeeCategoryResponse>> create(
            @Valid @RequestBody FeeCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Fee category created", categoryService.create(req)));
    }

    @PutMapping("/{id}")
    @RequirePermission(submodule = Submodule.ACCOUNTS_FEE_CATEGORIES, action = "UPDATE")
    public ResponseEntity<ApiResponse<FeeCategoryResponse>> update(
            @PathVariable Long id, @Valid @RequestBody FeeCategoryRequest req) {
        return ResponseEntity.ok(new ApiResponse<>("Fee category updated", categoryService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(submodule = Submodule.ACCOUNTS_FEE_CATEGORIES, action = "DELETE")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>("Fee category deleted", null));
    }

    // ── Pricing matrix ──────────────────────────────────────────────────────

    @GetMapping("/{id}/pricing")
    @RequirePermission(submodule = Submodule.ACCOUNTS_FEE_CATEGORIES, action = "READ")
    public ResponseEntity<ApiResponse<List<FeePricingRow>>> getPricing(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("OK", pricingService.getMatrix(id)));
    }

    @PutMapping("/{id}/pricing")
    @RequirePermission(submodule = Submodule.ACCOUNTS_FEE_CATEGORIES, action = "UPDATE")
    public ResponseEntity<ApiResponse<List<FeePricingRow>>> updatePricing(
            @PathVariable Long id, @RequestBody List<FeePricingRow> rows) {
        return ResponseEntity.ok(new ApiResponse<>("Pricing updated", pricingService.updateMatrix(id, rows)));
    }
}
