package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.RoomRequestDto;
import com.example.academic_service.entity.Room;
import com.example.academic_service.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<ApiResponse<Room>> create(@Valid @RequestBody RoomRequestDto dto) {
        return ResponseEntity.ok(roomService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Room>> update(@PathVariable Integer id,
                                                    @Valid @RequestBody RoomRequestDto dto) {
        return ResponseEntity.ok(roomService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Room>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(roomService.getById(id));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Room>>> getAll(
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(roomService.getAll(active));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<Room>> reactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(roomService.reactivate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(roomService.delete(id));
    }
}