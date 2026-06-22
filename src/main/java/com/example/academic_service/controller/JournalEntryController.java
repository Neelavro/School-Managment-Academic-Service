package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.dto.JournalEntryRequest;
import com.example.academic_service.dto.JournalEntryResponse;
import com.example.academic_service.entity.JournalReferenceType;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.JournalEntryService;
import com.example.academic_service.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/accounting/journal-entries")
@RequiredArgsConstructor
public class JournalEntryController {

    private final JournalEntryService service;

    @GetMapping
    @RequirePermission(submodule = Submodule.ACCOUNTS_JOURNAL, action = "READ")
    public ResponseEntity<ApiResponse<Page<JournalEntryResponse>>> search(
            @RequestParam(required = false) JournalReferenceType referenceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.search(referenceType, from, to, page, size)));
    }

    @GetMapping("/{id}")
    @RequirePermission(submodule = Submodule.ACCOUNTS_JOURNAL, action = "READ")
    public ResponseEntity<ApiResponse<JournalEntryResponse>> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.getOne(id)));
    }

    @PostMapping
    @RequirePermission(submodule = Submodule.ACCOUNTS_JOURNAL, action = "CREATE")
    public ResponseEntity<ApiResponse<JournalEntryResponse>> post(@Valid @RequestBody JournalEntryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Journal entry posted",
                        service.post(req, currentUser())));
    }

    @PostMapping("/{id}/reverse")
    @RequirePermission(submodule = Submodule.ACCOUNTS_JOURNAL, action = "CREATE")
    public ResponseEntity<ApiResponse<JournalEntryResponse>> reverse(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Entry reversed",
                        service.reverse(id, reason, currentUser())));
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
