package com.example.academic_service.dto.exam_dtos;

import com.example.academic_service.entity.ExamClassRoomAssignment;
import com.example.academic_service.entity.Room;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExamClassRoomAssignmentResponseDto {

    private Integer id;
    private Integer examRoutineId;
    private Integer classId;
    private String className;
    private Integer roomId;
    private String roomName;
    private Integer roomCapacity;
    private Integer startRoll;
    private Integer endRoll;

    public static ExamClassRoomAssignmentResponseDto from(ExamClassRoomAssignment a) {
        ExamClassRoomAssignmentResponseDto dto = new ExamClassRoomAssignmentResponseDto();
        dto.setId(a.getId());
        dto.setExamRoutineId(a.getExamRoutine().getId());
        dto.setClassId(a.getExamClass().getId());
        dto.setClassName(a.getExamClass().getName());
        Room room = a.getRoom();
        if (room != null) {
            dto.setRoomId(room.getId());
            dto.setRoomName(room.getName());
            dto.setRoomCapacity(room.getCapacity());
        }
        dto.setStartRoll(a.getStartRoll());
        dto.setEndRoll(a.getEndRoll());
        return dto;
    }
}
