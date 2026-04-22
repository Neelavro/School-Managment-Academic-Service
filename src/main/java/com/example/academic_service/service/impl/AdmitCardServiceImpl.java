package com.example.academic_service.service.impl;

import com.example.academic_service.dto.admit_card_dtos.AdmitCardAllocationDto;
import com.example.academic_service.dto.admit_card_dtos.AdmitCardRoutineResponseDto;
import com.example.academic_service.dto.admit_card_dtos.AdmitCardSessionDto;
import com.example.academic_service.dto.admit_card_dtos.AdmitCardStudentDto;
import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AdmitCardServiceImpl {

    private final ExamRoutineRepository        examRoutineRepository;
    private final ExamSessionRepository        examSessionRepository;
    private final ExamSeatAllocationRepository examSeatAllocationRepository;
    private final EnrollmentRepository         enrollmentRepository;
    private final RoomRepository               roomRepository;

    public AdmitCardRoutineResponseDto getAdmitCardData(
            Integer routineId,
            Integer sessionId,
            Integer classId,
            Integer genderSectionId,
            Long sectionId
    ) {
        // ── Routine ──
        ExamRoutine routine = examRoutineRepository.findById(routineId)
                .orElseThrow(() -> new RuntimeException("Routine not found"));

        // ── All active sessions for this routine ──
        List<ExamSession> allSessions =
                examSessionRepository.findByExamRoutineIdAndIsActiveTrue(routineId);

        // ── Build full schedule per classId ──
        Map<Integer, List<AdmitCardSessionDto>> fullScheduleByClassId = new HashMap<>();
        for (ExamSession s : allSessions) {
            if (s.getExamClass() == null) continue;
            fullScheduleByClassId
                    .computeIfAbsent(s.getExamClass().getId(), k -> new ArrayList<>())
                    .add(new AdmitCardSessionDto(
                            s.getId(),
                            s.getSubject() != null ? s.getSubject().getName() : null,
                            s.getDate().toString(),
                            s.getStartTime().toString(),
                            s.getEndTime().toString(),
                            s.getExamClass().getName(),
                            s.getGroup() != null ? s.getGroup().getId() : null,  // ← add
                            new ArrayList<>(),
                            null
                    ));
        }

        // ── Filtered sessions (determines which students get a card) ──
        List<ExamSession> filteredSessions = new ArrayList<>(allSessions);

        if (sessionId != null)
            filteredSessions = filteredSessions.stream()
                    .filter(s -> s.getId().equals(sessionId))
                    .toList();

        if (classId != null)
            filteredSessions = filteredSessions.stream()
                    .filter(s -> s.getExamClass() != null &&
                            s.getExamClass().getId().equals(classId))
                    .toList();

        // ── Room cache ──
        Map<Integer, Room> roomCache = new HashMap<>();
        roomRepository.findAll().forEach(r -> roomCache.put(r.getId(), r));

        // ── Build session DTOs from filtered sessions ──
        List<AdmitCardSessionDto> sessionDtos = new ArrayList<>();

        for (ExamSession session : filteredSessions) {

            List<ExamSeatAllocation> allocations =
                    examSeatAllocationRepository.findByExamSessionId(session.getId());

            if (genderSectionId != null)
                allocations = allocations.stream()
                        .filter(a -> a.getGenderSection() != null &&
                                a.getGenderSection().getId().equals(genderSectionId))
                        .toList();

            if (sectionId != null)
                allocations = allocations.stream()
                        .filter(a -> a.getSection() != null &&
                                a.getSection().getId().equals(sectionId))
                        .toList();

            List<AdmitCardAllocationDto> allocationDtos = new ArrayList<>();

            for (ExamSeatAllocation alloc : allocations) {

                Room room = alloc.getRoomId() != null
                        ? roomCache.get(alloc.getRoomId())
                        : null;

                Specification<Enrollment> spec = EnrollmentSpecification.filter(
                        null,
                        session.getExamClass() != null
                                ? session.getExamClass().getId() : null,
                        alloc.getSection() != null
                                ? alloc.getSection().getId() : null,
                        null,
                        alloc.getGenderSection() != null
                                ? alloc.getGenderSection().getId() : null,
                        null,
                        true,
                        null,
                        alloc.getStartRoll(),
                        alloc.getEndRoll()
                );

                List<Enrollment> enrollments = enrollmentRepository.findAll(spec);

                List<AdmitCardStudentDto> students = enrollments.stream()
                        .sorted(Comparator.comparingInt(e ->
                                e.getClassRoll() != null ? e.getClassRoll() : 0))
                        .map(e -> new AdmitCardStudentDto(
                                e.getStudentSystemId(),
                                e.getClassRoll()
                        ))
                        .toList();

                allocationDtos.add(new AdmitCardAllocationDto(
                        alloc.getId(),
                        alloc.getRoomId(),
                        room != null ? room.getName() : null,
                        alloc.getStartRoll(),
                        alloc.getEndRoll(),
                        alloc.getSection() != null
                                ? alloc.getSection().getSectionName() : null,
                        alloc.getGenderSection() != null
                                ? alloc.getGenderSection().getGenderName() : null,
                        students
                ));
            }

            // Attach full class schedule to this session
            List<AdmitCardSessionDto> fullSchedule = session.getExamClass() != null
                    ? fullScheduleByClassId.get(session.getExamClass().getId())
                    : new ArrayList<>();

            sessionDtos.add(new AdmitCardSessionDto(
                    session.getId(),
                    session.getSubject() != null ? session.getSubject().getName() : null,
                    session.getDate().toString(),
                    session.getStartTime().toString(),
                    session.getEndTime().toString(),
                    session.getExamClass().getName(),
                    session.getGroup() != null ? session.getGroup().getId() : null,  // ← add
                    new ArrayList<>(),
                    null
            ));
        }

        return new AdmitCardRoutineResponseDto(
                routine.getId(),
                routine.getTitle(),
                routine.getExamType() != null
                        ? routine.getExamType().getName() : null,
                routine.getAcademicYear() != null
                        ? routine.getAcademicYear().getYearName() : null,
                routine.getStatus() != null
                        ? routine.getStatus().name() : null,
                routine.getIsActive(),
                routine.getCreatedAt(),
                routine.getLastModifiedAt(),
                sessionDtos
        );
    }
    public AdmitCardRoutineResponseDto getAdmitCardDataBySection(
            Integer routineId,
            Integer sessionId,
            Integer classId,
            Integer genderSectionId,
            Long sectionId,
            Integer groupId            // ← add
    ) {
        ExamRoutine routine = examRoutineRepository.findById(routineId)
                .orElseThrow(() -> new RuntimeException("Routine not found"));

        List<ExamSession> allSessions =
                examSessionRepository.findByExamRoutineIdAndIsActiveTrue(routineId);

        // Build full schedule per classId
        Map<Integer, List<AdmitCardSessionDto>> fullScheduleByClassId = new HashMap<>();
        for (ExamSession s : allSessions) {
            if (s.getExamClass() == null) continue;
            fullScheduleByClassId
                    .computeIfAbsent(s.getExamClass().getId(), k -> new ArrayList<>())
                    .add(new AdmitCardSessionDto(
                            s.getId(),
                            s.getSubject() != null ? s.getSubject().getName() : null,
                            s.getDate().toString(),
                            s.getStartTime().toString(),
                            s.getEndTime().toString(),
                            s.getExamClass().getName(),
                            s.getGroup() != null ? s.getGroup().getId() : null,
                            new ArrayList<>(),
                            null
                    ));
        }

        // Filter sessions by sessionId and classId
        List<ExamSession> filteredSessions = new ArrayList<>(allSessions);

        if (sessionId != null)
            filteredSessions = filteredSessions.stream()
                    .filter(s -> s.getId().equals(sessionId))
                    .toList();

        if (classId != null)
            filteredSessions = filteredSessions.stream()
                    .filter(s -> s.getExamClass() != null &&
                            s.getExamClass().getId().equals(classId))
                    .toList();

        List<AdmitCardSessionDto> sessionDtos = new ArrayList<>();

        for (ExamSession session : filteredSessions) {

            Specification<Enrollment> spec = EnrollmentSpecification.filter(
                    null,
                    session.getExamClass() != null ? session.getExamClass().getId() : null,
                    sectionId,
                    null,
                    genderSectionId,
                    groupId,        // ← pass groupId
                    true,
                    null,
                    null,
                    null
            );

            List<Enrollment> enrollments = enrollmentRepository.findAll(spec);

            // Pull section/genderSection names only if the param was explicitly passed
            String resolvedSectionName = (sectionId != null)
                    ? enrollments.stream()
                    .filter(e -> e.getSection() != null)
                    .map(e -> e.getSection().getSectionName())
                    .findFirst().orElse(null)
                    : null;

            String resolvedGenderSectionName = (genderSectionId != null)
                    ? enrollments.stream()
                    .filter(e -> e.getGenderSection() != null)
                    .map(e -> e.getGenderSection().getGenderName())
                    .findFirst().orElse(null)
                    : null;

            List<AdmitCardStudentDto> students = enrollments.stream()
                    .sorted(Comparator.comparingInt(e ->
                            e.getClassRoll() != null ? e.getClassRoll() : 0))
                    .map(e -> new AdmitCardStudentDto(
                            e.getStudentSystemId(),
                            e.getClassRoll()
                    ))
                    .toList();

            AdmitCardAllocationDto allocationDto = new AdmitCardAllocationDto(
                    null, null, null, null, null,
                    resolvedSectionName,         // ← from enrollments
                    resolvedGenderSectionName,   // ← from enrollments
                    students
            );

            List<AdmitCardSessionDto> fullSchedule = session.getExamClass() != null
                    ? fullScheduleByClassId.get(session.getExamClass().getId())
                    : new ArrayList<>();

            sessionDtos.add(new AdmitCardSessionDto(
                    session.getId(),
                    session.getSubject() != null ? session.getSubject().getName() : null,
                    session.getDate().toString(),
                    session.getStartTime().toString(),
                    session.getEndTime().toString(),
                    session.getExamClass() != null ? session.getExamClass().getName() : null,
                    session.getGroup() != null ? session.getGroup().getId() : null,
                    List.of(allocationDto),  // ← was new ArrayList<>()
                    fullSchedule             // ← was null
            ));
        }

        return new AdmitCardRoutineResponseDto(
                routine.getId(),
                routine.getTitle(),
                routine.getExamType() != null ? routine.getExamType().getName() : null,
                routine.getAcademicYear() != null ? routine.getAcademicYear().getYearName() : null,
                routine.getStatus() != null ? routine.getStatus().name() : null,
                routine.getIsActive(),
                routine.getCreatedAt(),
                routine.getLastModifiedAt(),
                sessionDtos
        );
    }
}