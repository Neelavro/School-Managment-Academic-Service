package com.example.academic_service.dto.duty_dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class WeeklyScheduleResponseDto {
    private String teacherName;
    private int totalPeriodsThisWeek;
    private int totalExamDutiesThisWeek;
    private Map<String, List<TeacherPeriodDutyResponseDto>> periodsByDay;
    private List<TeacherExamDutyResponseDto> examDutiesThisWeek;
}
