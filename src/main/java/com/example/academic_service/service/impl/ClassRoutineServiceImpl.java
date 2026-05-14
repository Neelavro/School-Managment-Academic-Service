package com.example.academic_service.service.impl;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.schedule_dtos.ClassRoutineRequestDto;
import com.example.academic_service.dto.schedule_dtos.ClassRoutineResponseDto;
import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import com.example.academic_service.service.ClassRoutineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassRoutineServiceImpl implements ClassRoutineService {

    private final ClassRoutineRepository classRoutineRepository;
    private final ClassRepository classRepository;
    private final GenderSectionRepository genderSectionRepository;
    private final SectionRepository sectionRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final RoomRepository roomRepository;

    @Override
    public ApiResponse<ClassRoutineResponseDto> create(ClassRoutineRequestDto dto) {
        String clashError = validateAndCheckClash(dto, null);
        if (clashError != null) return ApiResponse.error(clashError);

        ClassRoutine routine = buildRoutine(new ClassRoutine(), dto);
        if (routine == null) return ApiResponse.error("One or more referenced entities not found");

        return ApiResponse.success("Routine slot created successfully",
                ClassRoutineResponseDto.from(classRoutineRepository.save(routine)));
    }

    @Override
    public ApiResponse<ClassRoutineResponseDto> update(Integer id, ClassRoutineRequestDto dto) {
        ClassRoutine existing = classRoutineRepository.findById(id).orElse(null);
        if (existing == null) return ApiResponse.error("Routine slot not found");
        if (!existing.getIsActive()) return ApiResponse.error("Cannot update an inactive slot");

        String clashError = validateAndCheckClash(dto, id);
        if (clashError != null) return ApiResponse.error(clashError);

        ClassRoutine updated = buildRoutine(existing, dto);
        if (updated == null) return ApiResponse.error("One or more referenced entities not found");

        return ApiResponse.success("Routine slot updated successfully",
                ClassRoutineResponseDto.from(classRoutineRepository.save(updated)));
    }

    @Override
    public ApiResponse<List<ClassRoutineResponseDto>> getAll(RoutineType routineType) {
        RoutineType type = routineType != null ? routineType : RoutineType.DEFAULT;
        List<ClassRoutineResponseDto> result = classRoutineRepository
                .findByRoutineTypeAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(type)
                .stream()
                .map(ClassRoutineResponseDto::from)
                .collect(Collectors.toList());
        return ApiResponse.success("Routine slots fetched successfully", result);
    }

    @Override
    public ApiResponse<Void> delete(Integer id) {
        ClassRoutine routine = classRoutineRepository.findById(id).orElse(null);
        if (routine == null) return ApiResponse.error("Routine slot not found");
        if (!routine.getIsActive()) return ApiResponse.error("Slot is already inactive");
        routine.setIsActive(false);
        classRoutineRepository.save(routine);
        return ApiResponse.success("Routine slot deleted successfully", null);
    }

    private String validateAndCheckClash(ClassRoutineRequestDto dto, Integer excludeId) {
        if (dto.getEndTime().isBefore(dto.getStartTime()) || dto.getEndTime().equals(dto.getStartTime())) {
            return "End time must be after start time";
        }

        LocalTime start = dto.getStartTime();
        LocalTime end = dto.getEndTime();

        // Room clash: same room, same day, overlapping time, same routine type
        boolean roomClash = classRoutineRepository
                .findByRoom_IdAndDayOfWeekAndRoutineTypeAndIsActiveTrue(dto.getRoomId(), dto.getDayOfWeek(), dto.getRoutineType())
                .stream()
                .filter(r -> excludeId == null || !r.getId().equals(excludeId))
                .anyMatch(r -> r.getStartTime().isBefore(end) && r.getEndTime().isAfter(start));

        if (roomClash) return "Room is already assigned to another class at this time";

        // Class clash: same class group, same day, overlapping time, same routine type
        boolean classClash = classRoutineRepository
                .findByClassEntity_IdAndGenderSection_IdAndDayOfWeekAndRoutineTypeAndIsActiveTrue(
                        dto.getClassId(), dto.getGenderSectionId(), dto.getDayOfWeek(), dto.getRoutineType())
                .stream()
                .filter(r -> excludeId == null || !r.getId().equals(excludeId))
                .filter(r -> {
                    Long existingSection = r.getSection() == null ? null : r.getSection().getId();
                    Integer existingGroup = r.getStudentGroup() == null ? null : r.getStudentGroup().getId();
                    return Objects.equals(dto.getSectionId(), existingSection)
                            && Objects.equals(dto.getStudentGroupId(), existingGroup);
                })
                .anyMatch(r -> r.getStartTime().isBefore(end) && r.getEndTime().isAfter(start));

        if (classClash) return "This class already has a slot at this time";

        return null;
    }

    private ClassRoutine buildRoutine(ClassRoutine routine, ClassRoutineRequestDto dto) {
        Class cls = classRepository.findById(dto.getClassId()).orElse(null);
        GenderSection genderSection = genderSectionRepository.findById(dto.getGenderSectionId()).orElse(null);
        Room room = roomRepository.findById(dto.getRoomId()).orElse(null);

        if (cls == null || genderSection == null || room == null) return null;

        routine.setClassEntity(cls);
        routine.setGenderSection(genderSection);
        routine.setRoom(room);
        routine.setDayOfWeek(dto.getDayOfWeek());
        routine.setStartTime(dto.getStartTime());
        routine.setEndTime(dto.getEndTime());
        routine.setRoutineType(dto.getRoutineType());

        if (dto.getSectionId() != null) {
            Section section = sectionRepository.findById(dto.getSectionId()).orElse(null);
            routine.setSection(section);
        } else {
            routine.setSection(null);
        }

        if (dto.getStudentGroupId() != null) {
            StudentGroup group = studentGroupRepository.findById(dto.getStudentGroupId()).orElse(null);
            routine.setStudentGroup(group);
        } else {
            routine.setStudentGroup(null);
        }

        return routine;
    }
}
