package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.ClassTeacherService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// @RestController — disabled on production-full-system
@RequestMapping("/api/class-teachers")
@RequiredArgsConstructor
public class ClassTeacherController {

    private final ClassTeacherService classTeacherService;

    @GetMapping
    @RequirePermission(submodule = Submodule.CLASS_TEACHER, action = "READ")
    public ResponseEntity<ApiResponse> getByYear(@RequestParam Integer academicYearId) {
        return ResponseEntity.ok(new ApiResponse("OK", classTeacherService.getByYear(academicYearId)));
    }

    @PostMapping
    @RequirePermission(submodule = Submodule.CLASS_TEACHER, action = "CREATE")
    public ResponseEntity<ApiResponse> assign(@RequestBody Map<String, Object> body) {
        Long staffId = ((Number) body.get("staffId")).longValue();
        Long sectionId = ((Number) body.get("sectionId")).longValue();
        Integer academicYearId = ((Number) body.get("academicYearId")).intValue();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Assigned", classTeacherService.assign(staffId, sectionId, academicYearId)));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(submodule = Submodule.CLASS_TEACHER, action = "DELETE")
    public ResponseEntity<ApiResponse> remove(@PathVariable Long id) {
        classTeacherService.remove(id);
        return ResponseEntity.ok(new ApiResponse("Removed", null));
    }

    // Teacher portal — returns own assignment (no FBAC, reads staffId from JWT)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getMyAssignment(@RequestParam Integer academicYearId) {
        Long staffId = resolveStaffId();
        return ResponseEntity.ok(new ApiResponse("OK",
                classTeacherService.getMyAssignment(staffId, academicYearId)));
    }

    private Long resolveStaffId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        Object raw = details != null ? details.get("staffId") : null;
        if (raw == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No staff record linked to this account");
        return ((Number) raw).longValue();
    }
}
