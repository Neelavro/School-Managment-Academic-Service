package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.LeaveBalance;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.repository.LeaveBalanceRepository;
import com.example.academic_service.repository.LeaveTypeRepository;
import com.example.academic_service.repository.StaffRepository;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leave-balances")
@RequiredArgsConstructor
public class LeaveBalanceController {

    private final LeaveBalanceRepository balanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final StaffRepository staffRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getMyBalances(Authentication auth) {
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        Object staffIdRaw = details != null ? details.get("staffId") : null;
        if (staffIdRaw == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("No staff record linked to this account", null));
        }
        Long staffId = ((Number) staffIdRaw).longValue();
        int year = LocalDate.now().getYear();

        // Ensure every active leave type has a balance row for this staff+year
        List<LeaveBalance> existing = balanceRepository.findByStaffIdAndYear(staffId, year);
        Set<Integer> coveredTypeIds = existing.stream()
                .map(b -> b.getLeaveType().getId())
                .collect(Collectors.toSet());

        var staff = staffRepository.findById(staffId).orElse(null);
        if (staff != null) {
            var activeTypes = leaveTypeRepository.findByIsActive(true);
            List<LeaveBalance> toCreate = activeTypes.stream()
                    .filter(lt -> !coveredTypeIds.contains(lt.getId()))
                    .map(lt -> {
                        LeaveBalance b = new LeaveBalance();
                        b.setStaff(staff);
                        b.setLeaveType(lt);
                        b.setYear(year);
                        b.setAllocatedDays(lt.getAnnualQuota() != null ? lt.getAnnualQuota() : 0);
                        b.setUsedDays(0);
                        b.setPendingDays(0);
                        return b;
                    })
                    .collect(Collectors.toList());
            if (!toCreate.isEmpty()) {
                List<LeaveBalance> saved = balanceRepository.saveAll(toCreate);
                existing = new java.util.ArrayList<>(existing);
                existing.addAll(saved);
            }
        }

        return ResponseEntity.ok(new ApiResponse("OK", existing));
    }

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
