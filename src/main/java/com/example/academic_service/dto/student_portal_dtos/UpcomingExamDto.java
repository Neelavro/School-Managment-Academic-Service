package com.example.academic_service.dto.student_portal_dtos;

import com.example.academic_service.entity.ExamSession;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpcomingExamDto {
    private Integer examSessionId;
    private String examRoutineTitle;
    private String subjectName;
    private String subjectCode;
    private String className;
    private String studentGroupName;
    private String date;
    private String startTime;
    private String endTime;
    private String routineStartDate;
    private String routineEndDate;
    private Boolean showOnAdmitCard;

    public static UpcomingExamDto from(ExamSession es) {
        UpcomingExamDto dto = new UpcomingExamDto();
        dto.setExamSessionId(es.getId());
        if (es.getExamRoutine() != null) {
            dto.setExamRoutineTitle(es.getExamRoutine().getTitle());
            if (es.getExamRoutine().getRoutineStartDate() != null)
                dto.setRoutineStartDate(es.getExamRoutine().getRoutineStartDate().toString());
            if (es.getExamRoutine().getRoutineEndDate() != null)
                dto.setRoutineEndDate(es.getExamRoutine().getRoutineEndDate().toString());
        }
        if (es.getSubject() != null) {
            dto.setSubjectName(es.getSubject().getName());
            dto.setSubjectCode(es.getSubject().getCode());
        }
        if (es.getExamClass() != null) dto.setClassName(es.getExamClass().getName());
        if (es.getGroup() != null) dto.setStudentGroupName(es.getGroup().getGroupName());
        if (es.getDate() != null) dto.setDate(es.getDate().toString());
        if (es.getStartTime() != null) dto.setStartTime(es.getStartTime().toString());
        if (es.getEndTime() != null) dto.setEndTime(es.getEndTime().toString());
        dto.setShowOnAdmitCard(es.getShowOnAdmitCard());
        return dto;
    }
}
