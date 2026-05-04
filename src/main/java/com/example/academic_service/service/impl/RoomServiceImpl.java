package com.example.academic_service.service.impl;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.RoomRequestDto;
import com.example.academic_service.entity.Room;
import com.example.academic_service.repository.RoomRepository;
import com.example.academic_service.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

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