package com.example.academic_service.service.impl;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.exam_dtos.BulkSessionUpdateItemDto;
import com.example.academic_service.dto.exam_dtos.ExamSessionRequestDto;
import com.example.academic_service.dto.exam_dtos.ExamSessionResponseDto;
import com.example.academic_service.dto.exam_dtos.ImportSessionsRequestDto;
import com.example.academic_service.dto.exam_dtos.ImportSessionsResultDto;
import com.example.academic_service.entity.*;
import com.example.academic_service.entity.Class;
import com.example.academic_service.repository.*;
import com.example.academic_service.service.AuditLogService;
import com.example.academic_service.service.ExamSessionService;
import com.example.academic_service.util.AuditHelper;
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
    private final AuditLogService auditLogService;

    private static String describe(ExamSession s) {
        if (s == null) return "exam session";
        String subj  = s.getSubject()   != null ? s.getSubject().getName()   : "?";
        String cls   = s.getExamClass() != null ? s.getExamClass().getName() : "?";
        return "session: " + subj + " — " + cls + " (id " + s.getId() + ")";
    }

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

        if (dto.getStartTime() != null && dto.getEndTime() != null
                && !dto.getStartTime().isBefore(dto.getEndTime()))
            return ApiResponse.error("Start time must be before end time");

        StudentGroup group = null;
        if (dto.getGroup() != null) {
            group = studentGroupRepository.findById(dto.getGroup()).orElse(null);
            if (group == null) return ApiResponse.error("Group not found");
        }

        ExamSession session = new ExamSession();
        session.setExamRoutine(routine);
        session.setExamClass(examClass);
        session.setSubject(subject);
        session.setGroup(group);
        session.setDate(dto.getDate());
        session.setStartTime(dto.getStartTime());
        session.setEndTime(dto.getEndTime());
        session.setShowOnAdmitCard(dto.getShowOnAdmitCard() != null ? dto.getShowOnAdmitCard() : true);

        ExamSession saved = examSessionRepository.save(session);
        ExamSessionResponseDto savedDto = ExamSessionResponseDto.from(saved);
        auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(),
                AuditActionType.CREATE, Submodule.EXAM_ROUTINES,
                "ExamSession", saved.getId().toString(),
                "Created " + describe(saved),
                null, AuditHelper.toJson(savedDto));
        return ApiResponse.success("Exam session created successfully", savedDto);
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

        if (dto.getStartTime() != null && dto.getEndTime() != null
                && !dto.getStartTime().isBefore(dto.getEndTime()))
            return ApiResponse.error("Start time must be before end time");

        StudentGroup group = null;
        if (dto.getGroup() != null) {
            group = studentGroupRepository.findById(dto.getGroup()).orElse(null);
            if (group == null) return ApiResponse.error("Group not found");
        }

        ExamSessionResponseDto before = ExamSessionResponseDto.from(session);

        session.setExamClass(examClass);
        session.setSubject(subject);
        session.setGroup(group);
        session.setDate(dto.getDate());
        session.setStartTime(dto.getStartTime());
        session.setEndTime(dto.getEndTime());
        if (dto.getShowOnAdmitCard() != null) session.setShowOnAdmitCard(dto.getShowOnAdmitCard());
        session.setLastModifiedAt(LocalDateTime.now());

        ExamSession saved = examSessionRepository.save(session);
        ExamSessionResponseDto after = ExamSessionResponseDto.from(saved);
        auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(),
                AuditActionType.UPDATE, Submodule.EXAM_ROUTINES,
                "ExamSession", saved.getId().toString(),
                "Updated " + describe(saved),
                AuditHelper.toJson(before), AuditHelper.toJson(after));
        return ApiResponse.success("Exam session updated successfully", after);
    }

    // ── bulkCreate ────────────────────────────────────────────────────────────

    @Override
    public ApiResponse<List<ExamSessionResponseDto>> bulkCreate(List<ExamSessionRequestDto> dtos) {
        List<ExamSessionResponseDto> results = new ArrayList<>();
        for (ExamSessionRequestDto dto : dtos) {
            ApiResponse<ExamSessionResponseDto> res = create(dto);
            if (res.getData() == null) return ApiResponse.error("Bulk create failed: " + res.getMessage());
            results.add(res.getData());
        }
        return ApiResponse.success("Bulk exam sessions created successfully", results);
    }

    // ── bulkUpdate ────────────────────────────────────────────────────────────

    @Override
    public ApiResponse<List<ExamSessionResponseDto>> bulkUpdate(List<BulkSessionUpdateItemDto> dtos) {
        List<ExamSessionResponseDto> results = new ArrayList<>();
        for (BulkSessionUpdateItemDto dto : dtos) {
            ExamSession session = examSessionRepository.findById(dto.getId()).orElse(null);
            if (session == null) return ApiResponse.error("Session not found: id=" + dto.getId());
            if (!session.getIsActive()) return ApiResponse.error("Cannot update inactive session id=" + dto.getId());

            if (dto.getStartTime() != null && dto.getEndTime() != null
                    && !dto.getStartTime().isBefore(dto.getEndTime()))
                return ApiResponse.error("Start time must be before end time for session id=" + dto.getId());

            ExamSessionResponseDto before = ExamSessionResponseDto.from(session);

            session.setDate(dto.getDate());
            session.setStartTime(dto.getStartTime());
            session.setEndTime(dto.getEndTime());
            if (dto.getShowOnAdmitCard() != null) session.setShowOnAdmitCard(dto.getShowOnAdmitCard());
            session.setLastModifiedAt(LocalDateTime.now());

            ExamSession saved = examSessionRepository.save(session);
            ExamSessionResponseDto after = ExamSessionResponseDto.from(saved);
            auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(),
                    AuditActionType.UPDATE, Submodule.EXAM_ROUTINES,
                    "ExamSession", saved.getId().toString(),
                    "Bulk-updated " + describe(saved),
                    AuditHelper.toJson(before), AuditHelper.toJson(after));
            results.add(after);
        }
        return ApiResponse.success("Bulk exam sessions updated successfully", results);
    }

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
        ExamSession saved = examSessionRepository.save(session);
        auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(),
                AuditActionType.UPDATE, Submodule.EXAM_ROUTINES,
                "ExamSession", saved.getId().toString(),
                "Reactivated " + describe(saved));
        return ApiResponse.success("Exam session reactivated successfully",
                ExamSessionResponseDto.from(saved));
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Override
    public ApiResponse<Void> delete(Integer id) {
        ExamSession session = examSessionRepository.findById(id).orElse(null);
        if (session == null) return ApiResponse.error("Exam session not found");
        if (!session.getIsActive()) return ApiResponse.error("Exam session is already inactive");

        ExamSessionResponseDto before = ExamSessionResponseDto.from(session);
        session.setIsActive(false);
        examSessionRepository.save(session);
        auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(),
                AuditActionType.DELETE, Submodule.EXAM_ROUTINES,
                "ExamSession", session.getId().toString(),
                "Deleted " + describe(session),
                AuditHelper.toJson(before), null);
        return ApiResponse.success("Exam session deleted successfully", null);
    }

    // ── importFromRoutine ────────────────────────────────────────────────────
    // Clone every active session on the source routine over to the target
    // routine. Dates are intentionally blanked out — a new routine almost
    // always sits on different dates from the one it's copied from, and
    // silently carrying them over is a footgun. Same (class, subject, group)
    // combos on the target are skipped so re-running the import is safe.

    @Override
    public ApiResponse<ImportSessionsResultDto> importFromRoutine(ImportSessionsRequestDto dto) {
        if (dto.getSourceRoutineId().equals(dto.getTargetRoutineId()))
            return ApiResponse.error("Source and target routines must be different");

        ExamRoutine source = examRoutineRepository.findById(dto.getSourceRoutineId()).orElse(null);
        if (source == null) return ApiResponse.error("Source routine not found");

        ExamRoutine target = examRoutineRepository.findById(dto.getTargetRoutineId()).orElse(null);
        if (target == null) return ApiResponse.error("Target routine not found");
        if (!target.getIsActive()) return ApiResponse.error("Cannot import into an inactive routine");

        List<ExamSession> sourceSessions = examSessionRepository
                .findByExamRoutineIdAndIsActiveTrue(source.getId());
        if (sourceSessions.isEmpty())
            return ApiResponse.error("Source routine has no active sessions to import");

        // Build a lookup of what the target already has so we can skip
        // duplicates without a per-session DB round trip.
        Set<String> existingKeys = examSessionRepository
                .findByExamRoutineIdAndIsActiveTrue(target.getId())
                .stream()
                .map(ExamSessionServiceImpl::sessionKey)
                .collect(Collectors.toSet());

        List<ExamSessionResponseDto> created = new ArrayList<>();
        int skipped = 0;
        for (ExamSession src : sourceSessions) {
            String key = sessionKey(src);
            if (existingKeys.contains(key)) {
                skipped++;
                continue;
            }

            ExamSession copy = new ExamSession();
            copy.setExamRoutine(target);
            copy.setExamClass(src.getExamClass());
            copy.setSubject(src.getSubject());
            copy.setGroup(src.getGroup());
            copy.setDate(null); // blanked deliberately
            copy.setStartTime(src.getStartTime());
            copy.setEndTime(src.getEndTime());
            copy.setShowOnAdmitCard(src.getShowOnAdmitCard() != null ? src.getShowOnAdmitCard() : true);

            ExamSession saved = examSessionRepository.save(copy);
            existingKeys.add(key);
            ExamSessionResponseDto savedDto = ExamSessionResponseDto.from(saved);
            created.add(savedDto);

            auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(),
                    AuditActionType.CREATE, Submodule.EXAM_ROUTINES,
                    "ExamSession", saved.getId().toString(),
                    "Imported " + describe(saved) + " from routine " + source.getId(),
                    null, AuditHelper.toJson(savedDto));
        }

        ImportSessionsResultDto result = new ImportSessionsResultDto(
                created.size(), skipped, created);
        String message = String.format(
                "Imported %d session(s)%s", created.size(),
                skipped > 0 ? ", skipped " + skipped + " duplicate(s)" : "");
        return ApiResponse.success(message, result);
    }

    private static String sessionKey(ExamSession s) {
        Integer cls  = s.getExamClass() != null ? s.getExamClass().getId() : null;
        Integer sub  = s.getSubject()   != null ? s.getSubject().getId()   : null;
        Integer grp  = s.getGroup()     != null ? s.getGroup().getId()     : null;
        return cls + "|" + sub + "|" + grp;
    }
}
