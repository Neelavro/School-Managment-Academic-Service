package com.example.academic_service.service.impl;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.exam_dtos.ExamClassRoomAssignmentRequestDto;
import com.example.academic_service.dto.exam_dtos.ExamClassRoomAssignmentResponseDto;
import com.example.academic_service.entity.Class;
import com.example.academic_service.entity.ExamClassRoomAssignment;
import com.example.academic_service.entity.ExamRoutine;
import com.example.academic_service.entity.Room;
import com.example.academic_service.repository.ClassRepository;
import com.example.academic_service.repository.ExamClassRoomAssignmentRepository;
import com.example.academic_service.repository.ExamRoutineRepository;
import com.example.academic_service.repository.RoomRepository;
import com.example.academic_service.service.ExamClassRoomAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamClassRoomAssignmentServiceImpl implements ExamClassRoomAssignmentService {

    private final ExamClassRoomAssignmentRepository repository;
    private final ExamRoutineRepository examRoutineRepository;
    private final ClassRepository classRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional
    public ApiResponse<List<ExamClassRoomAssignmentResponseDto>> assign(ExamClassRoomAssignmentRequestDto dto) {
        ExamRoutine routine = examRoutineRepository.findById(dto.getExamRoutineId()).orElse(null);
        if (routine == null) return ApiResponse.error("Exam routine not found");
        if (!routine.getIsActive()) return ApiResponse.error("Cannot assign rooms to an inactive routine");

        Class examClass = classRepository.findById(dto.getClassId()).orElse(null);
        if (examClass == null) return ApiResponse.error("Class not found");

        if (dto.getRooms() == null || dto.getRooms().isEmpty()) {
            repository.deleteByExamRoutineIdAndExamClassId(dto.getExamRoutineId(), dto.getClassId());
            return ApiResponse.success("Room assignments cleared", List.of());
        }

        List<Integer> roomIds = dto.getRooms().stream()
                .map(ExamClassRoomAssignmentRequestDto.RoomSlotDto::getRoomId)
                .collect(Collectors.toList());
        List<Room> rooms = roomRepository.findAllById(roomIds);
        if (rooms.size() != roomIds.size())
            return ApiResponse.error("One or more rooms not found");

        Map<Integer, Room> roomMap = rooms.stream()
                .collect(Collectors.toMap(Room::getId, r -> r));

        repository.deleteByExamRoutineIdAndExamClassId(dto.getExamRoutineId(), dto.getClassId());

        List<ExamClassRoomAssignment> saved = new ArrayList<>();
        for (ExamClassRoomAssignmentRequestDto.RoomSlotDto slot : dto.getRooms()) {
            ExamClassRoomAssignment assignment = new ExamClassRoomAssignment();
            assignment.setExamRoutine(routine);
            assignment.setExamClass(examClass);
            assignment.setRoom(roomMap.get(slot.getRoomId()));
            assignment.setStartRoll(slot.getStartRoll());
            assignment.setEndRoll(slot.getEndRoll());
            saved.add(repository.save(assignment));
        }

        return ApiResponse.success("Rooms assigned successfully",
                saved.stream().map(ExamClassRoomAssignmentResponseDto::from).collect(Collectors.toList()));
    }

    @Override
    public ApiResponse<List<ExamClassRoomAssignmentResponseDto>> getByRoutine(Integer routineId) {
        List<ExamClassRoomAssignment> assignments = repository.findByExamRoutineId(routineId);
        return ApiResponse.success("Room assignments fetched successfully",
                assignments.stream().map(ExamClassRoomAssignmentResponseDto::from).collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public ApiResponse<Void> removeByClassAndRoutine(Integer routineId, Integer classId) {
        repository.deleteByExamRoutineIdAndExamClassId(routineId, classId);
        return ApiResponse.success("Room assignments removed successfully", null);
    }
}
