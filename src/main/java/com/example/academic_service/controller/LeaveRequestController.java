package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.ApprovalAction;
import com.example.academic_service.entity.LeaveRequest;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.LeaveRequestService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @GetMapping("/staff/{staffId}")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "READ")
    public ResponseEntity<ApiResponse> getByStaff(@PathVariable Long staffId) {
        return ResponseEntity.ok(new ApiResponse("OK", leaveRequestService.getByStaff(staffId)));
    }

    @GetMapping("/pending")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "READ")
    public ResponseEntity<ApiResponse> getPending() {
        return ResponseEntity.ok(new ApiResponse("OK", leaveRequestService.getPending()));
    }

    @PostMapping("/staff/{staffId}")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "CREATE")
    public ResponseEntity<ApiResponse> submit(@PathVariable Long staffId, @RequestBody LeaveRequest req) {
        return ResponseEntity.ok(new ApiResponse("Submitted", leaveRequestService.submit(staffId, req)));
    }

    @PostMapping("/{requestId}/approve")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "UPDATE")
    public ResponseEntity<ApiResponse> approve(@PathVariable Long requestId,
                                                @RequestBody Map<String, Object> body,
                                                Authentication auth) {
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        Long userId = ((Number) details.get("userId")).longValue();
        ApprovalAction action = ApprovalAction.valueOf((String) body.get("action"));
        String remarks = (String) body.getOrDefault("remarks", null);
        return ResponseEntity.ok(new ApiResponse("Processed",
                leaveRequestService.processApproval(requestId, userId, action, remarks)));
    }
}
