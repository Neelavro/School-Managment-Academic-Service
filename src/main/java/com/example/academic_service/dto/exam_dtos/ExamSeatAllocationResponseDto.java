package com.example.academic_service.dto.exam_dtos;

import com.example.academic_service.entity.ExamSeatAllocation;
import com.example.academic_service.entity.GenderSection;
import com.example.academic_service.entity.Room;
import com.example.academic_service.entity.Section;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
public class ExamSeatAllocationResponseDto {

    private Integer id;
    private Integer examSessionId;
    private Section section;
    private GenderSection genderSection;
    private Room room;
    private Integer startRoll;
    private Integer endRoll;
    private LocalDateTime lastModifiedAt;

    public static ExamSeatAllocationResponseDto from(ExamSeatAllocation allocation, Room room) {
        ExamSeatAllocationResponseDto dto = new ExamSeatAllocationResponseDto();
        dto.setId(allocation.getId());
        dto.setExamSessionId(allocation.getExamSession().getId());
        dto.setSection(allocation.getSection());
        dto.setGenderSection(allocation.getGenderSection());
        dto.setRoom(room);
        dto.setStartRoll(allocation.getStartRoll());
        dto.setEndRoll(allocation.getEndRoll());
        dto.setLastModifiedAt(allocation.getLastModifiedAt());
        return dto;
    }
}