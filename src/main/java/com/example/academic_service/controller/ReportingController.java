package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.dto.reports.*;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.ReportingService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/accounting/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService service;

    @GetMapping("/ledger")
    @RequirePermission(submodule = Submodule.ACCOUNTS_REPORTS, action = "READ")
    public ResponseEntity<ApiResponse<LedgerReportDto>> ledger(
            @RequestParam Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.getLedger(accountId, from, to)));
    }

    @GetMapping("/trial-balance")
    @RequirePermission(submodule = Submodule.ACCOUNTS_REPORTS, action = "READ")
    public ResponseEntity<ApiResponse<TrialBalanceDto>> trialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.getTrialBalance(asOf)));
    }

    @GetMapping("/income-statement")
    @RequirePermission(submodule = Submodule.ACCOUNTS_REPORTS, action = "READ")
    public ResponseEntity<ApiResponse<IncomeStatementDto>> incomeStatement(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.getIncomeStatement(from, to)));
    }

    @GetMapping("/balance-sheet")
    @RequirePermission(submodule = Submodule.ACCOUNTS_REPORTS, action = "READ")
    public ResponseEntity<ApiResponse<BalanceSheetDto>> balanceSheet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(new ApiResponse<>("OK", service.getBalanceSheet(asOf)));
    }
}
