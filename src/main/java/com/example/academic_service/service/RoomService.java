package com.example.academic_service.service;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.RoomRequestDto;
import com.example.academic_service.entity.Room;

import java.util.List;

public interface RoomService {
    ApiResponse<Room> create(RoomRequestDto dto);
    ApiResponse<Room> update(Integer id, RoomRequestDto dto);
    ApiResponse<Room> getById(Integer id);
    ApiResponse<List<Room>> getAll(Boolean active);
    ApiResponse<Room> reactivate(Integer id);
    ApiResponse<Void> delete(Integer id);
}