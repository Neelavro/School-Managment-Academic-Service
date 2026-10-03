package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.dto.PayrollDtos.*;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.PayrollRunService;
import com.example.academic_service.service.PayrollSetupService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Payroll.
 *   Accounts (ACCOUNTS_PAYROLL): /setup/* — salary parts, designation salaries + bonuses, accounts;
 *                                /runs/*  — monthly salaries and festival bonuses.
 *   HR (HR_PAYROLL):             /staff/* — employees' own salaries and bonuses, their payslips.
 */
@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollSetupService setup;
    private final PayrollRunService runs;

    // ── Setup ──────────────────────────────────────────────────────────────

    @GetMapping("/setup/components")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYROLL, action = "READ")
    public ResponseEntity<ApiResponse<List<ComponentResponse>>> components() {
        return ok(setup.components());
    }

    @PostMapping("/setup/components")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYROLL, action = "CREATE")
    public ResponseEntity<ApiResponse<ComponentResponse>> createComponent(@RequestBody ComponentRequest req) {
        return ok("Salary part added", setup.saveComponent(null, req));
    }

    @PutMapping("/setup/components/{id}")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYROLL, action = "UPDATE")
    public ResponseEntity<ApiResponse<ComponentResponse>> updateComponent(@PathVariable Long id, @RequestBody ComponentRequest req) {
        return ok("Salary part saved", setup.saveComponent(id, req));
    }

    @DeleteMapping("/setup/components/{id}")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYROLL, action = "DELETE")
    public ResponseEntity<ApiResponse<Void>> deleteComponent(@PathVariable Long id) {
        setup.deleteComponent(id);
        return ok("Salary part deleted", null);
    }

    @GetMapping("/setup/designations")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYROLL, action = "READ")
    public ResponseEntity<ApiResponse<List<DesignationPay>>> designations() {
        return ok(setup.designations());
    }

    @PutMapping("/setup/designations/{designationId}")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYROLL, action = "UPDATE")
    public ResponseEntity<ApiResponse<DesignationPay>> saveDesignation(@PathVariable Integer designationId,
                                                                       @RequestBody DesignationPayRequest req) {
        return ok("Salary saved", setup.saveDesignation(designationId, req));
    }

    @GetMapping("/setup/settings")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYROLL, action = "READ")
    public ResponseEntity<ApiResponse<SettingsResponse>> settings() {
        return ok(setup.settings());
    }

    @PutMapping("/setup/settings")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYROLL, action = "UPDATE")
    public ResponseEntity<ApiResponse<SettingsResponse>> saveSettings(@RequestBody SettingsDto req) {
        return ok("Payroll accounts saved", setup.saveSettings(req));
    }

    // ── Staff salaries (HR) ────────────────────────────────────────────────

    @GetMapping("/staff")
    @RequirePermission(submodule = Submodule.HR_PAYROLL, action = "READ")
    public ResponseEntity<ApiResponse<List<StaffPayRow>>> staff(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return ok(setup.staff(month));
    }

    /** Salary parts the HR screen offers (read with HR permission, so HR doesn't need Accounts access). */
    @GetMapping("/staff/components")
    @RequirePermission(submodule = Submodule.HR_PAYROLL, action = "READ")
    public ResponseEntity<ApiResponse<List<ComponentResponse>>> staffComponents() {
        return ok(setup.components());
    }

    @GetMapping("/staff/{staffId}")
    @RequirePermission(submodule = Submodule.HR_PAYROLL, action = "READ")
    public ResponseEntity<ApiResponse<StaffPayDetail>> staffDetail(
            @PathVariable Long staffId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return ok(setup.staffDetail(staffId, month));
    }

    @PutMapping("/staff/{staffId}/salary")
    @RequirePermission(submodule = Submodule.HR_PAYROLL, action = "UPDATE")
    public ResponseEntity<ApiResponse<StaffPayDetail>> saveStaffSalary(@PathVariable Long staffId, @RequestBody StaffSalaryRequest req) {
        return ok("Salary saved", setup.saveStaffSalary(staffId, req, user()));
    }

    @DeleteMapping("/staff/{staffId}/salary")
    @RequirePermission(submodule = Submodule.HR_PAYROLL, action = "DELETE")
    public ResponseEntity<ApiResponse<StaffPayDetail>> deleteStaffSalaryMonth(
            @PathVariable Long staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        setup.deleteStaffSalaryMonth(staffId, month);
        return ok("Change removed", setup.staffDetail(staffId, null));
    }

    @PutMapping("/staff/{staffId}/bonus")
    @RequirePermission(submodule = Submodule.HR_PAYROLL, action = "UPDATE")
    public ResponseEntity<ApiResponse<StaffPayDetail>> saveStaffBonus(@PathVariable Long staffId, @RequestBody(required = false) BonusRuleDto bonus) {
        return ok("Festival bonus saved", setup.saveStaffBonus(staffId, bonus));
    }

    @GetMapping("/staff/{staffId}/payslips")
    @RequirePermission(submodule = Submodule.HR_PAYROLL, action = "READ")
    public ResponseEntity<ApiResponse<List<StaffPayslip>>> staffPayslips(@PathVariable Long staffId) {
        return ok(runs.forStaff(staffId));
    }

    // ── Runs ───────────────────────────────────────────────────────────────

    @GetMapping("/runs")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYROLL, action = "READ")
    public ResponseEntity<ApiResponse<List<RunSummary>>> listRuns() {
        return ok(runs.list());
    }

    @GetMapping("/runs/{id}")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYROLL, action = "READ")
    public ResponseEntity<ApiResponse<RunDetail>> getRun(@PathVariable Long id) {
        return ok(runs.get(id));
    }

    @PostMapping("/runs")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYROLL, action = "CREATE")
    public ResponseEntity<ApiResponse<RunDetail>> createRun(@RequestBody RunCreateRequest req) {
        return ok("Payroll created", runs.create(req, user()));
    }

    @PostMapping("/runs/{id}/rebuild")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYROLL, action = "UPDATE")
    public ResponseEntity<ApiResponse<RunDetail>> rebuild(@PathVariable Long id) {
        return ok("Payslips made again from the salary setup", runs.rebuild(id));
    }

    @DeleteMapping("/runs/{id}")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYROLL, action = "DELETE")
    public ResponseEntity<ApiResponse<Void>> deleteRun(@PathVariable Long id) {
        runs.deleteDraft(id);
        return ok("Draft deleted", null);
    }

    @PostMapping("/runs/{id}/finalise")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYROLL, action = "UPDATE")
    public ResponseEntity<ApiResponse<RunDetail>> finalise(@PathVariable Long id) {
        return ok("Payroll finalised", runs.finalise(id, user()));
    }

    @PostMapping("/runs/{id}/cancel")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYROLL, action = "DELETE")
    public ResponseEntity<ApiResponse<RunDetail>> cancel(@PathVariable Long id) {
        return ok("Payroll cancelled", runs.cancel(id, user()));
    }

    @PostMapping("/runs/{id}/pay")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYROLL, action = "CREATE")
    public ResponseEntity<ApiResponse<RunDetail>> pay(@PathVariable Long id, @RequestBody PayRequest req) {
        return ok("Paid", runs.pay(id, req, user()));
    }

    @PutMapping("/payslips/{payslipId}/lines")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYROLL, action = "UPDATE")
    public ResponseEntity<ApiResponse<PayslipDto>> updateLines(@PathVariable Long payslipId, @RequestBody PayslipLinesRequest req) {
        return ok("Payslip saved", runs.updateLines(payslipId, req));
    }

    @DeleteMapping("/payslips/{payslipId}")
    @RequirePermission(submodule = Submodule.ACCOUNTS_PAYROLL, action = "UPDATE")
    public ResponseEntity<ApiResponse<Void>> removePayslip(@PathVariable Long payslipId) {
        runs.removePayslip(payslipId);
        return ok("Removed from this payroll", null);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ok("OK", data);
    }

    private static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(new ApiResponse<>(message, data));
    }

    private static String user() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
