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

    private final ExamRoutineRepository examRoutineRepository;
    private final ExamSessionRepository examSessionRepository;
    private final EnrollmentRepository  enrollmentRepository;

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

        // ── Build full schedule per classId (only scheduled admit-card sessions) ──
        Map<Integer, List<AdmitCardSessionDto>> fullScheduleByClassId = new HashMap<>();
        for (ExamSession s : allSessions) {
            if (s.getExamClass() == null) continue;
            if (Boolean.FALSE.equals(s.getShowOnAdmitCard())) continue;
            fullScheduleByClassId
                    .computeIfAbsent(s.getExamClass().getId(), k -> new ArrayList<>())
                    .add(new AdmitCardSessionDto(
                            s.getId(),
                            s.getSubject() != null ? s.getSubject().getName() : null,
                            s.getDate() != null ? s.getDate().toString() : null,
                            s.getStartTime() != null ? s.getStartTime().toString() : null,
                            s.getEndTime() != null ? s.getEndTime().toString() : null,
                            s.getExamClass().getName(),
                            s.getGroup() != null ? s.getGroup().getId() : null,
                            new ArrayList<>(),
                            null
                    ));
        }

        // ── Filtered sessions (determines which students get a card) ──
        List<ExamSession> filteredSessions = allSessions.stream()
                .filter(s -> !Boolean.FALSE.equals(s.getShowOnAdmitCard()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        if (sessionId != null)
            filteredSessions = filteredSessions.stream()
                    .filter(s -> s.getId().equals(sessionId))
                    .toList();

        if (classId != null)
            filteredSessions = filteredSessions.stream()
                    .filter(s -> s.getExamClass() != null &&
                            s.getExamClass().getId().equals(classId))
                    .toList();

        // ── Build session DTOs from filtered sessions ──
        List<AdmitCardSessionDto> sessionDtos = new ArrayList<>();

        for (ExamSession session : filteredSessions) {

            List<AdmitCardSessionDto> fullSchedule = session.getExamClass() != null
                    ? fullScheduleByClassId.get(session.getExamClass().getId())
                    : new ArrayList<>();

            sessionDtos.add(new AdmitCardSessionDto(
                    session.getId(),
                    session.getSubject() != null ? session.getSubject().getName() : null,
                    session.getDate() != null ? session.getDate().toString() : null,
                    session.getStartTime() != null ? session.getStartTime().toString() : null,
                    session.getEndTime() != null ? session.getEndTime().toString() : null,
                    session.getExamClass() != null ? session.getExamClass().getName() : null,
                    session.getGroup() != null ? session.getGroup().getId() : null,
                    new ArrayList<>(),
                    fullSchedule
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

        // Build full schedule per classId (only scheduled admit-card sessions)
        Map<Integer, List<AdmitCardSessionDto>> fullScheduleByClassId = new HashMap<>();
        for (ExamSession s : allSessions) {
            if (s.getExamClass() == null) continue;
            if (Boolean.FALSE.equals(s.getShowOnAdmitCard())) continue;
            fullScheduleByClassId
                    .computeIfAbsent(s.getExamClass().getId(), k -> new ArrayList<>())
                    .add(new AdmitCardSessionDto(
                            s.getId(),
                            s.getSubject() != null ? s.getSubject().getName() : null,
                            s.getDate() != null ? s.getDate().toString() : null,
                            s.getStartTime() != null ? s.getStartTime().toString() : null,
                            s.getEndTime() != null ? s.getEndTime().toString() : null,
                            s.getExamClass().getName(),
                            s.getGroup() != null ? s.getGroup().getId() : null,
                            new ArrayList<>(),
                            null
                    ));
        }

        // Filter sessions by sessionId and classId (only admit-card sessions)
        List<ExamSession> filteredSessions = allSessions.stream()
                .filter(s -> !Boolean.FALSE.equals(s.getShowOnAdmitCard()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

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
                    session.getDate() != null ? session.getDate().toString() : null,
                    session.getStartTime() != null ? session.getStartTime().toString() : null,
                    session.getEndTime() != null ? session.getEndTime().toString() : null,
                    session.getExamClass() != null ? session.getExamClass().getName() : null,
                    session.getGroup() != null ? session.getGroup().getId() : null,
                    List.of(allocationDto),
                    fullSchedule
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