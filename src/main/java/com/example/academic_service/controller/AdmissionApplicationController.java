package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.AdmissionApplicationService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Admin side of online admission: review submitted applications. Uses the Students permission. */
@RestController
@RequestMapping("/api/admission-applications")
@RequiredArgsConstructor
public class AdmissionApplicationController {

    private final AdmissionApplicationService service;

    @GetMapping
    @RequirePermission(submodule = Submodule.STUDENTS, action = "READ")
    public ResponseEntity<ApiResponse> list(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) Integer classId,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new ApiResponse("OK", service.list(status, classId, q, page, size)));
    }

    @GetMapping("/{id}")
    @RequirePermission(submodule = Submodule.STUDENTS, action = "READ")
    public ResponseEntity<ApiResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse("OK", service.get(id)));
    }

    @PostMapping("/{id}/approve")
    @RequirePermission(submodule = Submodule.STUDENTS, action = "UPDATE")
    public ResponseEntity<ApiResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse("Approved", service.approve(id, reviewer())));
    }

    /** Body: {note?} */
    @PostMapping("/{id}/reject")
    @RequirePermission(submodule = Submodule.STUDENTS, action = "UPDATE")
    public ResponseEntity<ApiResponse> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Object note = body == null ? null : body.get("note");
        return ResponseEntity.ok(new ApiResponse("Rejected", service.reject(id, note == null ? null : note.toString(), reviewer())));
    }

    private static String reviewer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }
}
