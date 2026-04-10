package com.example.academic_service.service.impl;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.AvailableRoomResponseDto;
import com.example.academic_service.dto.RoomRequestDto;
import com.example.academic_service.entity.ExamSeatAllocation;
import com.example.academic_service.entity.ExamSession;
import com.example.academic_service.entity.Room;
import com.example.academic_service.repository.ExamSeatAllocationRepository;
import com.example.academic_service.repository.ExamSessionRepository;
import com.example.academic_service.repository.RoomRepository;
import com.example.academic_service.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamSeatAllocationRepository examSeatAllocationRepository;

    @Override
    public ApiResponse<Room> create(RoomRequestDto dto) {
        if (roomRepository.existsByNameIgnoreCase(dto.getName())) {
            return ApiResponse.error("Room with this name already exists");
        }
        Room room = new Room();
        room.setName(dto.getName());
        room.setCapacity(dto.getCapacity());
        return ApiResponse.success("Room created successfully", roomRepository.save(room));
    }

    @Override
    public ApiResponse<Room> update(Integer id, RoomRequestDto dto) {
        Room room = roomRepository.findById(id).orElse(null);
        if (room == null) return ApiResponse.error("Room not found");

        if (!room.getIsActive()) return ApiResponse.error("Cannot update an inactive room");

        if (roomRepository.existsByNameIgnoreCaseAndIdNot(dto.getName(), id)) {
            return ApiResponse.error("Another room with this name already exists");
        }
        room.setName(dto.getName());
        room.setCapacity(dto.getCapacity());
        return ApiResponse.success("Room updated successfully", roomRepository.save(room));
    }

    @Override
    public ApiResponse<Room> getById(Integer id) {
        Room room = roomRepository.findById(id).orElse(null);
        if (room == null) return ApiResponse.error("Room not found");
        return ApiResponse.success("Room fetched successfully", room);
    }

    @Override
    public ApiResponse<List<Room>> getAll(Boolean active) {
        List<Room> result;
        if (active == null) {
            result = roomRepository.findAll();
        } else if (active) {
            result = roomRepository.findByIsActiveTrue();
        } else {
            result = roomRepository.findByIsActiveFalse();
        }
        return ApiResponse.success("Rooms fetched successfully", result);
    }

    // service/RoomService.java
    public List<AvailableRoomResponseDto> getAvailableRooms(
            LocalDate date, LocalTime startTime, LocalTime endTime,
            Integer startRoll, Integer endRoll) {

        int requestedCapacity = endRoll - startRoll + 1;

        List<Room> allRooms = roomRepository.findByIsActiveTrue();

        // Step 1: Find sessions overlapping the time slot
        List<ExamSession> overlappingSessions =
                examSessionRepository.findOverlappingSessionsOnDate(date, startTime, endTime);

        // Step 2: Get all seat allocations for those overlapping sessions
        Map<Integer, Integer> usedCapacityByRoomId = new HashMap<>();

        if (!overlappingSessions.isEmpty()) {
            List<Integer> overlappingSessionIds = overlappingSessions.stream()
                    .map(ExamSession::getId)
                    .toList();

            List<ExamSeatAllocation> allocations =
                    examSeatAllocationRepository.findByExamSessionIdIn(overlappingSessionIds);

            // Step 3: Calculate used capacity per room
            for (ExamSeatAllocation alloc : allocations) {
                if (alloc.getRoomId() == null) continue;
                if (alloc.getStartRoll() == null || alloc.getEndRoll() == null) continue;

                int used = alloc.getEndRoll() - alloc.getStartRoll() + 1;
                usedCapacityByRoomId.merge(alloc.getRoomId(), used, Integer::sum);
            }
        }

        // Step 4: Build available rooms response
        List<AvailableRoomResponseDto> result = new ArrayList<>();

        for (Room room : allRooms) {
            int used = usedCapacityByRoomId.getOrDefault(room.getId(), 0);
            int available = room.getCapacity() - used;
            boolean availableToBook = available >= requestedCapacity;

            result.add(new AvailableRoomResponseDto(
                    room.getId(),
                    room.getName(),
                    room.getCapacity(),
                    used,
                    available,
                    availableToBook
            ));
        }

        return result;
    }
    @Override
    public ApiResponse<Room> reactivate(Integer id) {
        Room room = roomRepository.findById(id).orElse(null);
        if (room == null) return ApiResponse.error("Room not found");

        if (room.getIsActive()) return ApiResponse.error("Room is already active");

        // Check if another active room with the same name exists
        if (roomRepository.existsByNameIgnoreCaseAndIsActiveTrueAndIdNot(room.getName(), id)) {
            return ApiResponse.error("An active room with the name '" + room.getName() + "' already exists. Rename before reactivating");
        }

        room.setIsActive(true);
        return ApiResponse.success("Room reactivated successfully", roomRepository.save(room));
    }

    @Override
    public ApiResponse<Void> delete(Integer id) {
        Room room = roomRepository.findById(id).orElse(null);
        if (room == null) return ApiResponse.error("Room not found");

        if (!room.getIsActive()) return ApiResponse.error("Room is already inactive");

        room.setIsActive(false);
        roomRepository.save(room);
        return ApiResponse.success("Room deleted successfully", null);
    }
}