package com.example.academic_service.dto.duty_dtos;

import com.example.academic_service.entity.ExamSession;
import com.example.academic_service.entity.Room;
import com.example.academic_service.entity.TeacherExamDuty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeacherExamDutyResponseDto {
    private Long id;
    private Integer examSessionId;
    private String examRoutineTitle;
    private String className;
    private String studentGroupName;
    private String subjectName;
    private String subjectCode;
    private String date;
    private String startTime;
    private String endTime;
    private Integer roomId;
    private String roomName;

    public static TeacherExamDutyResponseDto from(TeacherExamDuty d) {
        ExamSession s = d.getExamSession();
        Room room = d.getRoom();
        TeacherExamDutyResponseDto dto = new TeacherExamDutyResponseDto();
        dto.setId(d.getId());
        dto.setExamSessionId(s.getId());
        dto.setExamRoutineTitle(s.getExamRoutine().getTitle());
        dto.setClassName(s.getExamClass().getName());
        if (s.getGroup() != null) dto.setStudentGroupName(s.getGroup().getGroupName());
        dto.setSubjectName(s.getSubject().getName());
        dto.setSubjectCode(s.getSubject().getCode());
        if (s.getDate() != null) dto.setDate(s.getDate().toString());
        if (s.getStartTime() != null) dto.setStartTime(s.getStartTime().toString());
        if (s.getEndTime() != null) dto.setEndTime(s.getEndTime().toString());
        dto.setRoomId(room.getId());
        dto.setRoomName(room.getName());
        return dto;
    }
}
