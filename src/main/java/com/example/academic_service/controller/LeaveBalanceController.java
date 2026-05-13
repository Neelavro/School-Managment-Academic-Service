package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.repository.LeaveBalanceRepository;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/leave-balances")
@RequiredArgsConstructor
public class LeaveBalanceController {

    private final LeaveBalanceRepository balanceRepository;

    @GetMapping("/staff/{staffId}")
    @RequirePermission(submodule = Submodule.HR_LEAVE_REQUESTS, action = "READ")
    public ResponseEntity<ApiResponse> getForStaffCurrentYear(@PathVariable Long staffId) {
        int year = LocalDate.now().getYear();
        return ResponseEntity.ok(new ApiResponse("OK", balanceRepository.findByStaffIdAndYear(staffId, year)));
    }

    @GetMapping("/staff/{staffId}/{year}")
    @RequirePermission(submodule = Submodule.HR_LEAVE_REQUESTS, action = "READ")
    public ResponseEntity<ApiResponse> getForStaffYear(@PathVariable Long staffId, @PathVariable Integer year) {
        return ResponseEntity.ok(new ApiResponse("OK", balanceRepository.findByStaffIdAndYear(staffId, year)));
    }
}
