package com.example.academic_service.controller;

import com.example.academic_service.dto.marking_dtos.*;
import com.example.academic_service.service.MarkingStructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/marking-structures")
@RequiredArgsConstructor
public class MarkingStructureController {

    private final MarkingStructureService markingStructureService;
    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> bulkCreate(@RequestBody BulkMarkingStructureRequest request) {
        return ResponseEntity.ok(markingStructureService.bulkCreate(request));
    }


    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody MarkingStructureRequest request) {
        return ResponseEntity.ok(markingStructureService.create(request));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getByFilters(
            @RequestParam(required = false) Integer examTypeId,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer subjectId,
            @RequestParam(required = false) Integer groupId) {
        return ResponseEntity.ok(markingStructureService.getByFilters(examTypeId, classId, subjectId, groupId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Integer id,
            @RequestBody MarkingStructureRequest request) {
        return ResponseEntity.ok(markingStructureService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(markingStructureService.delete(id));
    }

}