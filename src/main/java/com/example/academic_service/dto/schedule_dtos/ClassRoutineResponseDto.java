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
    private SubjectInfo subject;
    private RoomInfo room;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private String routineType;
    private Boolean isActive;

    public static ClassRoutineResponseDto from(ClassRoutine r) {
        ClassRoutineResponseDto dto = new ClassRoutineResponseDto();
        dto.setId(r.getId());
        if (r.getClassEntity() != null) {
            dto.setClassEntity(new ClassInfo(r.getClassEntity().getId(), r.getClassEntity().getName()));
        }
        // gender_section_id, section_id, student_group_id, subject_id and
        // room_id are all NULLable in the schema (FKs with ON DELETE SET NULL).
        // Older rows may have any of them missing; guard each so the response
        // serializer never NPEs on legacy data.
        if (r.getGenderSection() != null) {
            dto.setGenderSection(new GenderSectionInfo(
                    r.getGenderSection().getId(), r.getGenderSection().getGenderName()));
        }
        if (r.getSection() != null) {
            dto.setSection(new SectionInfo(r.getSection().getId(), r.getSection().getSectionName()));
        }
        if (r.getStudentGroup() != null) {
            dto.setStudentGroup(new StudentGroupInfo(r.getStudentGroup().getId(), r.getStudentGroup().getGroupName()));
        }
        if (r.getSubject() != null) {
            dto.setSubject(new SubjectInfo(r.getSubject().getId(), r.getSubject().getName(), r.getSubject().getCode()));
        }
        if (r.getRoom() != null) {
            dto.setRoom(new RoomInfo(r.getRoom().getId(), r.getRoom().getName()));
        }
        dto.setDayOfWeek(r.getDayOfWeek() != null ? r.getDayOfWeek().name() : null);
        dto.setStartTime(r.getStartTime() != null ? r.getStartTime().toString() : null);
        dto.setEndTime(r.getEndTime() != null ? r.getEndTime().toString() : null);
        dto.setRoutineType(r.getRoutineType() != null ? r.getRoutineType().name() : null);
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
    public static class SubjectInfo {
        private Integer id;
        private String name;
        private String code;
    }

    @Getter
    @AllArgsConstructor
    public static class RoomInfo {
        private Integer id;
        private String name;
    }
}
