package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.service.GrandFinalPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Publish Results → Grand Final: release, hide and update a class's Grand Final for the student portal. */
@RestController
@RequestMapping("/api/grand-final/{academicYearId}")
@RequiredArgsConstructor
public class GrandFinalPublishController {

    private final GrandFinalPublishService service;

    /** Refusals (already published, job running, not found…) answer 400 with the reason. */
    private static <T> T run(Supplier<T> action) {
        try {
            return action.get();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/classes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> status(@PathVariable Integer academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("Grand Final status fetched", run(() -> service.status(academicYearId))));
    }

    @PatchMapping("/publish")
    public ResponseEntity<ApiResponse<Void>> publish(@PathVariable Integer academicYearId, @RequestParam Integer classId) {
        return ResponseEntity.ok(ApiResponse.success(run(() -> service.publish(academicYearId, classId)), null));
    }

    @PatchMapping("/unpublish")
    public ResponseEntity<ApiResponse<Void>> unpublish(@PathVariable Integer academicYearId, @RequestParam Integer classId) {
        return ResponseEntity.ok(ApiResponse.success(run(() -> service.unpublish(academicYearId, classId)), null));
    }

    @PostMapping("/changes/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> check(@PathVariable Integer academicYearId, @RequestParam Integer classId) {
        long jobId = run(() -> service.check(academicYearId, classId));
        return ResponseEntity.ok(ApiResponse.success("Checking the Grand Final", Map.of("jobId", jobId)));
    }

    @GetMapping("/changes/{jobId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changes(@PathVariable Integer academicYearId, @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Grand Final changes fetched", service.changes(academicYearId, jobId, page, size)));
    }

    @PostMapping("/changes/{jobId}/apply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> apply(@PathVariable Integer academicYearId, @PathVariable Long jobId) {
        int[] c = service.apply(academicYearId, jobId);
        return ResponseEntity.ok(ApiResponse.success("Grand Final updated", Map.of("changed", c[0], "added", c[1], "removed", c[2])));
    }

    @DeleteMapping("/changes/{jobId}")
    public ResponseEntity<ApiResponse<Void>> discard(@PathVariable Integer academicYearId, @PathVariable Long jobId) {
        service.discard(academicYearId, jobId);
        return ResponseEntity.ok(ApiResponse.success("Changes discarded", null));
    }
}
