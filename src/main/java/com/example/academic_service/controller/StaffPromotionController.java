package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.Designation;
import com.example.academic_service.entity.StaffPromotion;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.StaffPromotionService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/staff-promotions")
@RequiredArgsConstructor
public class StaffPromotionController {

    private final StaffPromotionService promotionService;

    @GetMapping("/staff/{staffId}")
    @RequirePermission(submodule = Submodule.HR_STAFF, action = "READ")
    public ResponseEntity<ApiResponse> getHistory(@PathVariable Long staffId) {
        return ResponseEntity.ok(new ApiResponse("OK", promotionService.getHistory(staffId)));
    }

    @PostMapping("/staff/{staffId}")
    @RequirePermission(submodule = Submodule.HR_STAFF, action = "CREATE")
    public ResponseEntity<ApiResponse> record(@PathVariable Long staffId, @RequestBody Map<String, Object> body) {
        StaffPromotion req = new StaffPromotion();
        Designation to = new Designation();
        to.setId(((Number) body.get("toDesignationId")).intValue());
        req.setToDesignation(to);
        req.setPromotionDate(LocalDate.parse((String) body.get("promotionDate")));
        req.setNotes((String) body.get("notes"));
        return ResponseEntity.ok(new ApiResponse("Recorded", promotionService.record(staffId, req)));
    }
}
