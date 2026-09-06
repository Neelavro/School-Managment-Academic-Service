package com.example.academic_service.controller;

import com.example.academic_service.service.StellarAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stellar")
@ConditionalOnProperty(name = "stellar.enabled", havingValue = "true")
@RequiredArgsConstructor
public class StellarAttendanceController {

    private final StellarAttendanceService service;

    @GetMapping("/today-count")
    public ResponseEntity<Map<String, Object>> todayCount() {
        StellarAttendanceService.TodayCount r = service.getTodayCount();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("count", r.count());
        body.put("fetchedAt", r.fetchedAt());
        body.put("cached", r.cached());
        return ResponseEntity.ok(body);
    }
}
