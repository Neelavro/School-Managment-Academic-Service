package com.example.academic_service.dto.duty_dtos;

import com.example.academic_service.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AvailableRoomDto {
    private Integer id;
    private String name;
    private Integer capacity;

    public static AvailableRoomDto from(Room r) {
        return new AvailableRoomDto(r.getId(), r.getName(), r.getCapacity());
    }
}
