package com.example.academic_service.controller;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.AvailableRoomResponseDto;
import com.example.academic_service.dto.RoomRequestDto;
import com.example.academic_service.entity.Room;
import com.example.academic_service.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
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

    @GetMapping("/available")
    public ResponseEntity<List<AvailableRoomResponseDto>> getAvailableRooms(
            @RequestParam LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam Integer startRoll,
            @RequestParam Integer endRoll) {

        List<AvailableRoomResponseDto> rooms =
                roomService.getAvailableRooms(date, startTime, endTime, startRoll, endRoll);
        return ResponseEntity.ok(rooms);
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