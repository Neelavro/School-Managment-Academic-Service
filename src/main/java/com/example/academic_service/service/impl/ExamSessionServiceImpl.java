package com.example.academic_service.service.impl;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.exam_dtos.ExamSessionRequestDto;
import com.example.academic_service.dto.exam_dtos.ExamSessionResponseDto;
import com.example.academic_service.entity.*;
import com.example.academic_service.entity.Class;
import com.example.academic_service.repository.*;
import com.example.academic_service.service.ExamSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamSessionServiceImpl implements ExamSessionService {

    private final ExamSessionRepository examSessionRepository;
    private final ExamRoutineRepository examRoutineRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final StudentGroupRepository studentGroupRepository;

    // ── create ───────────────────────────────────────────────────────────────

        @Override
        public ApiResponse<ExamSessionResponseDto> create(ExamSessionRequestDto dto) {
            ExamRoutine routine = examRoutineRepository.findById(dto.getExamRoutineId()).orElse(null);
            if (routine == null) return ApiResponse.error("Exam routine not found");
            if (!routine.getIsActive()) return ApiResponse.error("Cannot add sessions to an inactive routine");

            Class examClass = classRepository.findById(dto.getClassId()).orElse(null);
            if (examClass == null) return ApiResponse.error("Class not found");

            Subject subject = subjectRepository.findById(dto.getSubjectId()).orElse(null);
            if (subject == null) return ApiResponse.error("Subject not found");

            if (!dto.getStartTime().isBefore(dto.getEndTime()))
                return ApiResponse.error("Start time must be before end time");

            // Optional group
            StudentGroup group = null;
            if (dto.getGroup() != null) {
                group = studentGroupRepository.findById(dto.getGroup()).orElse(null);
                if (group == null) return ApiResponse.error("Group not found");
            }

            ExamSession session = new ExamSession();
            session.setExamRoutine(routine);
            session.setExamClass(examClass);
            session.setSubject(subject);
            session.setGroup(group);           // ← add
            session.setDate(dto.getDate());
            session.setStartTime(dto.getStartTime());
            session.setEndTime(dto.getEndTime());

            return ApiResponse.success("Exam session created successfully",
                    ExamSessionResponseDto.from(examSessionRepository.save(session)));
        }

        @Override
        public ApiResponse<ExamSessionResponseDto> update(Integer id, ExamSessionRequestDto dto) {
            ExamSession session = examSessionRepository.findById(id).orElse(null);
            if (session == null) return ApiResponse.error("Exam session not found");
            if (!session.getIsActive()) return ApiResponse.error("Cannot update an inactive exam session");

            Class examClass = classRepository.findById(dto.getClassId()).orElse(null);
            if (examClass == null) return ApiResponse.error("Class not found");

            Subject subject = subjectRepository.findById(dto.getSubjectId()).orElse(null);
            if (subject == null) return ApiResponse.error("Subject not found");

            if (!dto.getStartTime().isBefore(dto.getEndTime()))
                return ApiResponse.error("Start time must be before end time");

            // Optional group — null means shared/all groups
            StudentGroup group = null;
            if (dto.getGroup() != null) {
                group = studentGroupRepository.findById(dto.getGroup()).orElse(null);
                if (group == null) return ApiResponse.error("Group not found");
            }

            session.setExamClass(examClass);
            session.setSubject(subject);
            session.setGroup(group);           // ← add
            session.setDate(dto.getDate());
            session.setStartTime(dto.getStartTime());
            session.setEndTime(dto.getEndTime());
            session.setLastModifiedAt(LocalDateTime.now());

            return ApiResponse.success("Exam session updated successfully",
                    ExamSessionResponseDto.from(examSessionRepository.save(session)));
        }

        // getByRoutine, reactivate, delete unchanged...


    // ── getByRoutine ─────────────────────────────────────────────────────────

    @Override
    public ApiResponse<List<ExamSessionResponseDto>> getByRoutine(Integer routineId, Boolean active) {
        ExamRoutine routine = examRoutineRepository.findById(routineId).orElse(null);
        if (routine == null) return ApiResponse.error("Exam routine not found");

        List<ExamSession> sessions;
        if (active == null) {
            sessions = examSessionRepository.findByExamRoutineId(routineId);
        } else if (active) {
            sessions = examSessionRepository.findByExamRoutineIdAndIsActiveTrue(routineId);
        } else {
            sessions = examSessionRepository.findByExamRoutineIdAndIsActiveFalse(routineId);
        }

        List<ExamSessionResponseDto> result = sessions.stream()
                .map(ExamSessionResponseDto::from)
                .collect(Collectors.toList());
        return ApiResponse.success("Exam sessions fetched successfully", result);
    }

    // ── reactivate ───────────────────────────────────────────────────────────

    @Override
    public ApiResponse<ExamSessionResponseDto> reactivate(Integer id) {
        ExamSession session = examSessionRepository.findById(id).orElse(null);
        if (session == null) return ApiResponse.error("Exam session not found");
        if (session.getIsActive()) return ApiResponse.error("Exam session is already active");
        if (!session.getExamRoutine().getIsActive())
            return ApiResponse.error("Cannot reactivate a session belonging to an inactive routine");

        session.setIsActive(true);
        session.setLastModifiedAt(LocalDateTime.now());
        return ApiResponse.success("Exam session reactivated successfully",
                ExamSessionResponseDto.from(examSessionRepository.save(session)));
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Override
    public ApiResponse<Void> delete(Integer id) {
        ExamSession session = examSessionRepository.findById(id).orElse(null);
        if (session == null) return ApiResponse.error("Exam session not found");
        if (!session.getIsActive()) return ApiResponse.error("Exam session is already inactive");

        session.setIsActive(false);
        examSessionRepository.save(session);
        return ApiResponse.success("Exam session deleted successfully", null);
    }
}