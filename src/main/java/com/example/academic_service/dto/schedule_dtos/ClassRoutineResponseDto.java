package com.example.academic_service.dto.schedule_dtos;

import com.example.academic_service.entity.ClassRoutine;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClassRoutineResponseDto {

    private Integer id;
    private ClassInfo classEntity;
    private GenderSectionInfo genderSection;
    private SectionInfo section;
    private StudentGroupInfo studentGroup;
    private RoomInfo room;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private String routineType;
    private Boolean isActive;

    public static ClassRoutineResponseDto from(ClassRoutine r) {
        ClassRoutineResponseDto dto = new ClassRoutineResponseDto();
        dto.setId(r.getId());
        dto.setClassEntity(new ClassInfo(r.getClassEntity().getId(), r.getClassEntity().getName()));
        dto.setGenderSection(new GenderSectionInfo(r.getGenderSection().getId(), r.getGenderSection().getGenderName()));
        if (r.getSection() != null) {
            dto.setSection(new SectionInfo(r.getSection().getId(), r.getSection().getSectionName()));
        }
        if (r.getStudentGroup() != null) {
            dto.setStudentGroup(new StudentGroupInfo(r.getStudentGroup().getId(), r.getStudentGroup().getGroupName()));
        }
        dto.setRoom(new RoomInfo(r.getRoom().getId(), r.getRoom().getName()));
        dto.setDayOfWeek(r.getDayOfWeek().name());
        dto.setStartTime(r.getStartTime().toString());
        dto.setEndTime(r.getEndTime().toString());
        dto.setRoutineType(r.getRoutineType().name());
        dto.setIsActive(r.getIsActive());
        return dto;
    }

    @Getter
    @AllArgsConstructor
    public static class ClassInfo {
        private Integer id;
        private String name;
    }

    @Getter
    @AllArgsConstructor
    public static class GenderSectionInfo {
        private Integer id;
        private String genderName;
    }

    @Getter
    @AllArgsConstructor
    public static class SectionInfo {
        private Long id;
        private String sectionName;
    }

    @Getter
    @AllArgsConstructor
    public static class StudentGroupInfo {
        private Integer id;
        private String groupName;
    }

    @Getter
    @AllArgsConstructor
    public static class RoomInfo {
        private Integer id;
        private String name;
    }
}
