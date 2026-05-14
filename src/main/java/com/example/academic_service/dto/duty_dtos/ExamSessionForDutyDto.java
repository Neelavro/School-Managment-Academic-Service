package com.example.academic_service.dto.duty_dtos;

import com.example.academic_service.entity.ExamSession;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExamSessionForDutyDto {
    private Integer id;
    private String examRoutineTitle;
    private Integer examRoutineId;
    private Integer classId;
    private String className;
    private String studentGroupName;
    private String subjectName;
    private String date;
    private String startTime;
    private String endTime;

    public static ExamSessionForDutyDto from(ExamSession s) {
        ExamSessionForDutyDto dto = new ExamSessionForDutyDto();
        dto.setId(s.getId());
        dto.setExamRoutineTitle(s.getExamRoutine().getTitle());
        dto.setExamRoutineId(s.getExamRoutine().getId());
        dto.setClassId(s.getExamClass().getId());
        dto.setClassName(s.getExamClass().getName());
        if (s.getGroup() != null) dto.setStudentGroupName(s.getGroup().getGroupName());
        dto.setSubjectName(s.getSubject().getName());
        if (s.getDate() != null) dto.setDate(s.getDate().toString());
        if (s.getStartTime() != null) dto.setStartTime(s.getStartTime().toString());
        if (s.getEndTime() != null) dto.setEndTime(s.getEndTime().toString());
        return dto;
    }
}
