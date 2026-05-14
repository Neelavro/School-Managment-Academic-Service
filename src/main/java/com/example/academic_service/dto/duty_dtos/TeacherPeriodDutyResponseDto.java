package com.example.academic_service.dto.duty_dtos;

import com.example.academic_service.entity.ClassRoutine;
import com.example.academic_service.entity.TeacherPeriodDuty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeacherPeriodDutyResponseDto {
    private Long id;
    private Integer classRoutineId;
    private Integer classId;
    private Integer subjectId;
    private Integer genderSectionId;
    private Long sectionId;
    private Integer groupId;
    private String className;
    private String genderSectionName;
    private String sectionName;
    private String studentGroupName;
    private String subjectName;
    private String subjectCode;
    private String dayOfWeek;
    private String startTime;
    private String endTime;

    public static TeacherPeriodDutyResponseDto from(TeacherPeriodDuty d) {
        ClassRoutine r = d.getClassRoutine();
        TeacherPeriodDutyResponseDto dto = new TeacherPeriodDutyResponseDto();
        dto.setId(d.getId());
        dto.setClassRoutineId(r.getId());
        dto.setClassId(r.getClassEntity().getId());
        dto.setGenderSectionId(r.getGenderSection().getId());
        dto.setClassName(r.getClassEntity().getName());
        dto.setGenderSectionName(r.getGenderSection().getGenderName());
        if (r.getSection() != null) {
            dto.setSectionId(r.getSection().getId());
            dto.setSectionName(r.getSection().getSectionName());
        }
        if (r.getStudentGroup() != null) {
            dto.setGroupId(r.getStudentGroup().getId());
            dto.setStudentGroupName(r.getStudentGroup().getGroupName());
        }
        if (r.getSubject() != null) {
            dto.setSubjectId(r.getSubject().getId());
            dto.setSubjectName(r.getSubject().getName());
            dto.setSubjectCode(r.getSubject().getCode());
        }
        dto.setDayOfWeek(r.getDayOfWeek().name());
        dto.setStartTime(r.getStartTime().toString());
        dto.setEndTime(r.getEndTime().toString());
        return dto;
    }
}
