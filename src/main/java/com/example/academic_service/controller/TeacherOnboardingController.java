package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.*;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.TeacherOnboardingService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher-onboarding")
@RequiredArgsConstructor
public class TeacherOnboardingController {

    private final TeacherOnboardingService onboardingService;

    // ── Academic Qualifications ───────────────────────────────────────────────

    @GetMapping("/staff/{staffId}/qualifications")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "READ")
    public ResponseEntity<ApiResponse> getQualifications(@PathVariable Long staffId) {
        return ResponseEntity.ok(new ApiResponse("OK", onboardingService.getQualifications(staffId)));
    }

    @PutMapping("/staff/{staffId}/qualifications")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "UPDATE")
    public ResponseEntity<ApiResponse> replaceQualifications(@PathVariable Long staffId,
                                                              @RequestBody List<AcademicQualification> list) {
        return ResponseEntity.ok(new ApiResponse("Saved", onboardingService.replaceQualifications(staffId, list)));
    }

    @PostMapping("/staff/{staffId}/qualifications")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "CREATE")
    public ResponseEntity<ApiResponse> addQualification(@PathVariable Long staffId,
                                                         @RequestBody AcademicQualification req) {
        return ResponseEntity.ok(new ApiResponse("Added", onboardingService.saveQualification(staffId, req)));
    }

    @DeleteMapping("/qualifications/{id}")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "DELETE")
    public ResponseEntity<ApiResponse> deleteQualification(@PathVariable Long id) {
        onboardingService.deleteQualification(id);
        return ResponseEntity.ok(new ApiResponse("Deleted", null));
    }

    // ── Teacher Profile (MPO / NTRCA / Experience) ───────────────────────────

    @GetMapping("/staff/{staffId}/profile")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "READ")
    public ResponseEntity<ApiResponse> getProfile(@PathVariable Long staffId) {
        return ResponseEntity.ok(new ApiResponse("OK", onboardingService.getProfile(staffId)));
    }

    @PutMapping("/staff/{staffId}/profile")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "UPDATE")
    public ResponseEntity<ApiResponse> saveProfile(@PathVariable Long staffId,
                                                    @RequestBody TeacherProfile req) {
        return ResponseEntity.ok(new ApiResponse("Saved", onboardingService.saveProfile(staffId, req)));
    }

    // ── Dependents ────────────────────────────────────────────────────────────

    @GetMapping("/staff/{staffId}/dependents")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "READ")
    public ResponseEntity<ApiResponse> getDependents(@PathVariable Long staffId) {
        return ResponseEntity.ok(new ApiResponse("OK", onboardingService.getDependents(staffId)));
    }

    @PutMapping("/staff/{staffId}/dependents")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "UPDATE")
    public ResponseEntity<ApiResponse> replaceDependents(@PathVariable Long staffId,
                                                          @RequestBody List<StaffDependent> list) {
        return ResponseEntity.ok(new ApiResponse("Saved", onboardingService.replaceDependents(staffId, list)));
    }

    @PostMapping("/staff/{staffId}/dependents")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "CREATE")
    public ResponseEntity<ApiResponse> addDependent(@PathVariable Long staffId,
                                                     @RequestBody StaffDependent req) {
        return ResponseEntity.ok(new ApiResponse("Added", onboardingService.addDependent(staffId, req)));
    }

    @DeleteMapping("/dependents/{id}")
    @RequirePermission(submodule = Submodule.HR_MANAGEMENT, action = "DELETE")
    public ResponseEntity<ApiResponse> deleteDependent(@PathVariable Long id) {
        onboardingService.deleteDependent(id);
        return ResponseEntity.ok(new ApiResponse("Deleted", null));
    }
}
