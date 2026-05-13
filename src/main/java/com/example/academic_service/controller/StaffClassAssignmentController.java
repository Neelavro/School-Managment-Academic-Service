package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.StaffClassAssignment;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.StaffClassAssignmentService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff-class-assignments")
@RequiredArgsConstructor
public class StaffClassAssignmentController {

    private final StaffClassAssignmentService assignmentService;

    @GetMapping("/staff/{staffId}")
    @RequirePermission(submodule = Submodule.HR_STAFF, action = "READ")
    public ResponseEntity<ApiResponse> getByStaff(@PathVariable Long staffId,
                                                   @RequestParam(required = false) Integer academicYearId) {
        var result = academicYearId != null
                ? assignmentService.getByStaffAndYear(staffId, academicYearId)
                : assignmentService.getByStaff(staffId);
        return ResponseEntity.ok(new ApiResponse("OK", result));
    }

    @GetMapping("/class/{classId}")
    @RequirePermission(submodule = Submodule.HR_STAFF, action = "READ")
    public ResponseEntity<ApiResponse> getByClass(@PathVariable Integer classId,
                                                   @RequestParam Integer academicYearId) {
        return ResponseEntity.ok(new ApiResponse("OK",
                assignmentService.getByClassAndYear(classId, academicYearId)));
    }

    @PostMapping
    @RequirePermission(submodule = Submodule.HR_STAFF, action = "CREATE")
    public ResponseEntity<ApiResponse> create(@RequestBody StaffClassAssignment req) {
        return ResponseEntity.ok(new ApiResponse("Created", assignmentService.create(req)));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(submodule = Submodule.HR_STAFF, action = "DELETE")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        assignmentService.delete(id);
        return ResponseEntity.ok(new ApiResponse("Deleted", null));
    }

    @PostMapping("/staff/{staffId}/copy")
    @RequirePermission(submodule = Submodule.HR_STAFF, action = "CREATE")
    public ResponseEntity<ApiResponse> copyFromYear(@PathVariable Long staffId,
                                                     @RequestParam Integer fromYearId,
                                                     @RequestParam Integer toYearId) {
        return ResponseEntity.ok(new ApiResponse("Copied",
                assignmentService.copyFromYear(staffId, fromYearId, toYearId)));
    }
}
