package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.dto.ChartOfAccountRequest;
import com.example.academic_service.dto.ChartOfAccountResponse;
import com.example.academic_service.dto.CsvImportResponse;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.ChartOfAccountService;
import com.example.academic_service.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/accounting/chart-of-accounts")
@RequiredArgsConstructor
public class ChartOfAccountController {

    private final ChartOfAccountService service;

    @GetMapping
    @RequirePermission(submodule = Submodule.ACCOUNTS_COA, action = "READ")
    public ResponseEntity<ApiResponse<List<ChartOfAccountResponse>>> getAll() {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.getAll()));
    }

    @GetMapping("/{id}")
    @RequirePermission(submodule = Submodule.ACCOUNTS_COA, action = "READ")
    public ResponseEntity<ApiResponse<ChartOfAccountResponse>> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.getOne(id)));
    }

    @PostMapping
    @RequirePermission(submodule = Submodule.ACCOUNTS_COA, action = "CREATE")
    public ResponseEntity<ApiResponse<ChartOfAccountResponse>> create(
            @Valid @RequestBody ChartOfAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Account created", service.create(request)));
    }

    @PutMapping("/{id}")
    @RequirePermission(submodule = Submodule.ACCOUNTS_COA, action = "UPDATE")
    public ResponseEntity<ApiResponse<ChartOfAccountResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ChartOfAccountRequest request) {
        return ResponseEntity.ok(new ApiResponse<>("Account updated", service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(submodule = Submodule.ACCOUNTS_COA, action = "DELETE")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiResponse<>("Account deleted", null));
    }

    /**
     * Bulk import via CSV.
     * Header (required, case-insensitive):
     *   account_code, account_name, account_type, parent_code, is_group, description
     *
     * If any row fails validation, no rows are persisted and the response lists per-row errors.
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(submodule = Submodule.ACCOUNTS_COA, action = "CREATE")
    public ResponseEntity<ApiResponse<CsvImportResponse>> importCsv(@RequestParam("file") MultipartFile file) {
        CsvImportResponse result = service.importCsv(file);
        String msg = result.isOk()
                ? "Imported " + result.getCreatedCount() + " accounts"
                : "Import rejected — " + result.getErrors().size() + " error(s)";
        return ResponseEntity.ok(new ApiResponse<>(msg, result));
    }
}
