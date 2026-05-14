package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.schedule_dtos.ClassRoutineRequestDto;
import com.example.academic_service.dto.schedule_dtos.ClassRoutineResponseDto;
import com.example.academic_service.entity.RoutineType;
import com.example.academic_service.service.ClassRoutineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/class-routines")
@RequiredArgsConstructor
public class ClassRoutineController {

    private final ClassRoutineService service;

    @PostMapping
    public ResponseEntity<ApiResponse<ClassRoutineResponseDto>> create(
            @Valid @RequestBody ClassRoutineRequestDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassRoutineResponseDto>> update(
            @PathVariable Integer id,
            @Valid @RequestBody ClassRoutineRequestDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClassRoutineResponseDto>>> getAll(
            @RequestParam(required = false) RoutineType routineType) {
        return ResponseEntity.ok(service.getAll(routineType));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(service.delete(id));
    }
}
