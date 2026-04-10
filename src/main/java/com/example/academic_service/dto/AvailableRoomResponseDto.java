package com.example.academic_service.dto;

import lombok.*;

// dto/response/AvailableRoomResponse.java
@Getter
@Setter
@AllArgsConstructor
public class AvailableRoomResponseDto {
    private Integer id;
    private String name;
    private Integer totalCapacity;
    private Integer usedCapacity;
    private Integer availableCapacity;
    private Boolean availableToBook;
}