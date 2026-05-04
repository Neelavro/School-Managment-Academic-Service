package com.example.academic_service.dto.exam_dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ExamClassRoomAssignmentRequestDto {

    @NotNull(message = "Exam routine is required")
    private Integer examRoutineId;

    @NotNull(message = "Class is required")
    private Integer classId;

    @NotNull(message = "Room list is required")
    private List<RoomSlotDto> rooms;

    @Getter
    @Setter
    public static class RoomSlotDto {
        @NotNull(message = "Room id is required")
        private Integer roomId;
        private Integer startRoll;
        private Integer endRoll;
    }
}
