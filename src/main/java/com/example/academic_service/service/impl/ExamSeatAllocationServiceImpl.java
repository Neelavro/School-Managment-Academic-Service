package com.example.academic_service.service.impl;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.AvailableRoomResponseDto;
import com.example.academic_service.dto.ExamSeatAllocationRequestDto;
import com.example.academic_service.dto.ExamSeatAllocationResponseDto;
import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamSeatAllocationServiceImpl {

    private final ExamSeatAllocationRepository allocationRepository;
    private final ExamSessionRepository examSessionRepository;
    private final SectionRepository sectionRepository;
    private final GenderSectionRepository genderSectionRepository;
    private final RoomRepository roomRepository;

    // ── helpers ───────────────────────────────────────────────────────────────

    private ExamSeatAllocationResponseDto toDto(ExamSeatAllocation allocation) {
        Room room = allocation.getRoomId() != null
                ? roomRepository.findById(allocation.getRoomId()).orElse(null)
                : null;
        return ExamSeatAllocationResponseDto.from(allocation, room);
    }

    private String validateRollRange(Integer startRoll, Integer endRoll) {
        if (startRoll == null || endRoll == null)
            return "Start roll and end roll are required";
        if (startRoll > endRoll)
            return "Start roll must be less than or equal to end roll";
        return null;
    }

    private String validateAllocationTarget(ExamSeatAllocationRequestDto dto) {
        if (dto.getSectionId() == null && dto.getGenderSectionId() == null)
            return "At least a section or gender section must be provided";
        return null;
    }

    // ── create ───────────────────────────────────────────────────────────────

    public ApiResponse<ExamSeatAllocationResponseDto> create(ExamSeatAllocationRequestDto dto) {
        ExamSession session = examSessionRepository.findById(dto.getExamSessionId()).orElse(null);
        if (session == null) return ApiResponse.error("Exam session not found");
        if (!session.getIsActive()) return ApiResponse.error("Cannot allocate to an inactive exam session");

        String rollError = validateRollRange(dto.getStartRoll(), dto.getEndRoll());
        if (rollError != null) return ApiResponse.error(rollError);

        String targetError = validateAllocationTarget(dto);
        if (targetError != null) return ApiResponse.error(targetError);

        ExamSeatAllocation allocation = new ExamSeatAllocation();
        allocation.setExamSession(session);
        allocation.setStartRoll(dto.getStartRoll());
        allocation.setEndRoll(dto.getEndRoll());

        if (dto.getSectionId() != null) {
            Section section = sectionRepository.findById(dto.getSectionId()).orElse(null);
            if (section == null) return ApiResponse.error("Section not found");
            allocation.setSection(section);
        }

        if (dto.getGenderSectionId() != null) {
            GenderSection gs = genderSectionRepository.findById(dto.getGenderSectionId()).orElse(null);
            if (gs == null) return ApiResponse.error("Gender section not found");
            allocation.setGenderSection(gs);
        }

        if (dto.getRoomId() != null) {
            Room room = roomRepository.findById(dto.getRoomId()).orElse(null);
            if (room == null) return ApiResponse.error("Room not found");
            if (!room.getIsActive()) return ApiResponse.error("Room '" + room.getName() + "' is inactive");
            allocation.setRoomId(dto.getRoomId());
        }

        return ApiResponse.success("Seat allocation created successfully",
                toDto(allocationRepository.save(allocation)));
    }

    // ── update ───────────────────────────────────────────────────────────────

    public ApiResponse<ExamSeatAllocationResponseDto> update(Integer id, ExamSeatAllocationRequestDto dto) {
        ExamSeatAllocation allocation = allocationRepository.findById(id).orElse(null);
        if (allocation == null) return ApiResponse.error("Seat allocation not found");
        if (!allocation.getExamSession().getIsActive())
            return ApiResponse.error("Cannot update allocation of an inactive exam session");

        String rollError = validateRollRange(dto.getStartRoll(), dto.getEndRoll());
        if (rollError != null) return ApiResponse.error(rollError);

        String targetError = validateAllocationTarget(dto);
        if (targetError != null) return ApiResponse.error(targetError);

        allocation.setStartRoll(dto.getStartRoll());
        allocation.setEndRoll(dto.getEndRoll());
        allocation.setLastModifiedAt(LocalDateTime.now());

        allocation.setSection(null);
        if (dto.getSectionId() != null) {
            Section section = sectionRepository.findById(dto.getSectionId()).orElse(null);
            if (section == null) return ApiResponse.error("Section not found");
            allocation.setSection(section);
        }

        allocation.setGenderSection(null);
        if (dto.getGenderSectionId() != null) {
            GenderSection gs = genderSectionRepository.findById(dto.getGenderSectionId()).orElse(null);
            if (gs == null) return ApiResponse.error("Gender section not found");
            allocation.setGenderSection(gs);
        }

        allocation.setRoomId(null);
        if (dto.getRoomId() != null) {
            Room room = roomRepository.findById(dto.getRoomId()).orElse(null);
            if (room == null) return ApiResponse.error("Room not found");
            if (!room.getIsActive()) return ApiResponse.error("Room '" + room.getName() + "' is inactive");
            allocation.setRoomId(dto.getRoomId());
        }

        return ApiResponse.success("Seat allocation updated successfully",
                toDto(allocationRepository.save(allocation)));
    }

    // ── getBySession ──────────────────────────────────────────────────────────

    public ApiResponse<List<ExamSeatAllocationResponseDto>> getBySession(Integer sessionId) {
        ExamSession session = examSessionRepository.findById(sessionId).orElse(null);
        if (session == null) return ApiResponse.error("Exam session not found");

        List<ExamSeatAllocationResponseDto> result = allocationRepository
                .findByExamSessionId(sessionId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ApiResponse.success("Seat allocations fetched successfully", result);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    public ApiResponse<Void> delete(Integer id) {
        ExamSeatAllocation allocation = allocationRepository.findById(id).orElse(null);
        if (allocation == null) return ApiResponse.error("Seat allocation not found");
        if (!allocation.getExamSession().getIsActive())
            return ApiResponse.error("Cannot delete allocation of an inactive exam session");

        allocationRepository.delete(allocation);
        return ApiResponse.success("Seat allocation deleted successfully", null);
    }

    // ── getAvailableRooms ─────────────────────────────────────────────────────

    public ApiResponse<List<AvailableRoomResponseDto>> getAvailableRooms(
            LocalDate date, LocalTime startTime, LocalTime endTime,
            Integer startRoll, Integer endRoll) {

        String rollError = validateRollRange(startRoll, endRoll);
        if (rollError != null) return ApiResponse.error(rollError);

        int requestedCapacity = endRoll - startRoll + 1;

        List<Room> allRooms = roomRepository.findByIsActiveTrue();

        List<ExamSeatAllocation> overlapping =
                allocationRepository.findOverlappingAllocations(date, startTime, endTime);

        Map<Integer, Integer> usedCapacityByRoomId = new HashMap<>();
        for (ExamSeatAllocation a : overlapping) {
            if (a.getRoomId() == null) continue;
            int students = a.getEndRoll() - a.getStartRoll() + 1;
            usedCapacityByRoomId.merge(a.getRoomId(), students, Integer::sum);
        }

        List<AvailableRoomResponseDto> result = allRooms.stream()
                .filter(room -> room.getCapacity() != null)
                .map(room -> {
                    int used = usedCapacityByRoomId.getOrDefault(room.getId(), 0);
                    int available = room.getCapacity() - used;
                    boolean availableToBook = available >= requestedCapacity;
                    return new AvailableRoomResponseDto(
                            room.getId(),
                            room.getName(),
                            room.getCapacity(),
                            used,
                            available,
                            availableToBook
                    );
                })
                .sorted(Comparator.comparing(AvailableRoomResponseDto::getAvailableCapacity).reversed())
                .collect(Collectors.toList());

        return ApiResponse.success("Available rooms fetched successfully", result);
    }
}