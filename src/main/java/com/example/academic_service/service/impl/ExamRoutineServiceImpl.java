package com.example.academic_service.service.impl;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.exam_dtos.CloneRoutineRequestDto;
import com.example.academic_service.dto.exam_dtos.ExamRoutineRequestDto;
import com.example.academic_service.entity.*;
import com.example.academic_service.entity.ResultPublication;
import com.example.academic_service.repository.AcademicYearRepository;
import com.example.academic_service.repository.ExamRoutineRepository;
import com.example.academic_service.repository.ExamSessionRepository;
import com.example.academic_service.repository.ExamTypeRepository;
import com.example.academic_service.repository.ResultPublicationRepository;
import com.example.academic_service.entity.AuditActionType;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.AuditLogService;
import com.example.academic_service.service.ResultPublishService;
import com.example.academic_service.service.ExamRoutineService;
import com.example.academic_service.util.AuditHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamRoutineServiceImpl implements ExamRoutineService {

    private final ExamRoutineRepository examRoutineRepository;
    private final ExamTypeRepository examTypeRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ResultPublicationRepository resultPublicationRepository;
    private final ResultPublishService resultPublishService;
    private final AuditLogService auditLogService;

    @Override
    public ApiResponse<ExamRoutine> create(ExamRoutineRequestDto dto) {
        ExamType examType = examTypeRepository.findById(dto.getExamTypeId()).orElse(null);
        if (examType == null) return ApiResponse.error("Exam type not found");
        if (!examType.getIsActive()) return ApiResponse.error("Cannot use an inactive exam type");

        AcademicYear academicYear = academicYearRepository.findById(dto.getAcademicYearId()).orElse(null);
        if (academicYear == null) return ApiResponse.error("Academic year not found");

        // Multiple routines per exam_type in one year IS allowed — e.g.
        // "1st Term / 2nd Term / 3rd Term" all sharing exam_type = "TERM
        // EXAM" and one marking structure. Uniqueness now enforced on
        // (title, academic_year) so accidental duplicates are still
        // prevented — the admin must pick distinct routine titles.
        String title = dto.getTitle() == null ? null : dto.getTitle().trim();
        if (title == null || title.isBlank()) {
            return ApiResponse.error("Routine title is required");
        }
        if (examRoutineRepository.existsByTitleAndAcademicYearIdAndIsActiveTrue(
                title, dto.getAcademicYearId())) {
            return ApiResponse.error("An active routine with this title already exists for this academic year");
        }
        ExamRoutine routine = new ExamRoutine();
        routine.setTitle(title);
        routine.setExamType(examType);
        routine.setAcademicYear(academicYear);
        routine.setRoutineStartDate(dto.getRoutineStartDate());
        routine.setRoutineEndDate(dto.getRoutineEndDate());
        routine.setConsiderForAnnualResult(dto.getConsiderForAnnualResult() != null ? dto.getConsiderForAnnualResult() : true);
        return ApiResponse.success("Exam routine created successfully", examRoutineRepository.save(routine));
    }

    @Override
    public ApiResponse<ExamRoutine> update(Integer id, ExamRoutineRequestDto dto) {
        ExamRoutine routine = examRoutineRepository.findById(id).orElse(null);
        if (routine == null) return ApiResponse.error("Exam routine not found");

        if (!routine.getIsActive()) return ApiResponse.error("Cannot update an inactive exam routine");

        ExamType examType = examTypeRepository.findById(dto.getExamTypeId()).orElse(null);
        if (examType == null) return ApiResponse.error("Exam type not found");
        if (!examType.getIsActive()) return ApiResponse.error("Cannot use an inactive exam type");

        AcademicYear academicYear = academicYearRepository.findById(dto.getAcademicYearId()).orElse(null);
        if (academicYear == null) return ApiResponse.error("Academic year not found");

        String title = dto.getTitle() == null ? null : dto.getTitle().trim();
        if (title == null || title.isBlank()) {
            return ApiResponse.error("Routine title is required");
        }
        if (examRoutineRepository.existsByTitleAndAcademicYearIdAndIsActiveTrueAndIdNot(
                title, dto.getAcademicYearId(), id)) {
            return ApiResponse.error("Another active routine with this title already exists for this academic year");
        }
        routine.setTitle(title);
        routine.setExamType(examType);
        routine.setAcademicYear(academicYear);
        routine.setRoutineStartDate(dto.getRoutineStartDate());
        routine.setRoutineEndDate(dto.getRoutineEndDate());
        routine.setConsiderForAnnualResult(dto.getConsiderForAnnualResult() != null ? dto.getConsiderForAnnualResult() : true);
        routine.setLastModifiedAt(LocalDateTime.now());
        return ApiResponse.success("Exam routine updated successfully", examRoutineRepository.save(routine));
    }

    private void hydrateResultPublished(List<ExamRoutine> routines) {
        if (routines.isEmpty()) return;
        List<Integer> ids = routines.stream().map(ExamRoutine::getId).collect(Collectors.toList());
        Set<Integer> publishedIds = resultPublicationRepository.findRoutineIdsWithAnyPublishedClass(ids);
        routines.forEach(r -> r.setResultPublished(publishedIds.contains(r.getId())));
    }

    private void hydrateResultPublished(ExamRoutine routine) {
        boolean anyPublished = resultPublicationRepository.findByExamRoutine_Id(routine.getId())
                .stream().anyMatch(rp -> Boolean.TRUE.equals(rp.getPublished()));
        routine.setResultPublished(anyPublished);
    }

    @Override
    public ApiResponse<ExamRoutine> getById(Integer id) {
        ExamRoutine routine = examRoutineRepository.findById(id).orElse(null);
        if (routine == null) return ApiResponse.error("Exam routine not found");
        hydrateResultPublished(routine);
        return ApiResponse.success("Exam routine fetched successfully", routine);
    }

    @Override
    public ApiResponse<List<ExamRoutine>> getAll(Boolean active) {
        List<ExamRoutine> result;
        if (active == null) {
            result = examRoutineRepository.findAll();
        } else if (active) {
            result = examRoutineRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        } else {
            result = examRoutineRepository.findByIsActiveFalseOrderByCreatedAtDesc();
        }
        hydrateResultPublished(result);
        return ApiResponse.success("Exam routines fetched successfully", result);
    }

    @Override
    public ApiResponse<List<ExamRoutine>> getByAcademicYear(Integer academicYearId) {
        List<ExamRoutine> result = examRoutineRepository.findByAcademicYearIdAndIsActiveTrue(academicYearId);
        hydrateResultPublished(result);
        return ApiResponse.success("Exam routines fetched successfully", result);
    }

    @Override
    public ApiResponse<List<ExamRoutine>> getByExamType(Integer examTypeId) {
        List<ExamRoutine> result = examRoutineRepository.findByExamTypeIdAndIsActiveTrue(examTypeId);
        hydrateResultPublished(result);
        return ApiResponse.success("Exam routines fetched successfully", result);
    }

    @Override
    public ApiResponse<ExamRoutine> publish(Integer id) {
        ExamRoutine routine = examRoutineRepository.findById(id).orElse(null);
        if (routine == null) return ApiResponse.error("Exam routine not found");

        if (!routine.getIsActive()) return ApiResponse.error("Cannot publish an inactive exam routine");

        if (routine.getStatus() == RoutineStatus.PUBLISHED) {
            return ApiResponse.error("Exam routine is already published");
        }
        routine.setStatus(RoutineStatus.PUBLISHED);
        routine.setPublishedAt(LocalDateTime.now());
        return ApiResponse.success("Exam routine published successfully", examRoutineRepository.save(routine));
    }

    @Override
    public ApiResponse<ExamRoutine> reactivate(Integer id) {
        ExamRoutine routine = examRoutineRepository.findById(id).orElse(null);
        if (routine == null) return ApiResponse.error("Exam routine not found");

        if (routine.getIsActive()) return ApiResponse.error("Exam routine is already active");

        if (examRoutineRepository.existsByExamTypeIdAndAcademicYearIdAndIsActiveTrueAndIdNot(
                routine.getExamType().getId(), routine.getAcademicYear().getId(), id)) {
            return ApiResponse.error("An active routine for '" + routine.getExamType().getName()
                    + "' in this academic year already exists. Deactivate it before reactivating this one");
        }
        routine.setIsActive(true);
        routine.setStatus(RoutineStatus.DRAFT);
        routine.setPublishedAt(null);
        routine.setLastModifiedAt(LocalDateTime.now());
        return ApiResponse.success("Exam routine reactivated successfully", examRoutineRepository.save(routine));
    }
    @Override
    public ApiResponse<ExamRoutine> unpublish(Integer id) {
        ExamRoutine routine = examRoutineRepository.findById(id).orElse(null);
        if (routine == null) return ApiResponse.error("Exam routine not found");

        if (!routine.getIsActive()) return ApiResponse.error("Cannot unpublish an inactive exam routine");

        if (routine.getStatus() == RoutineStatus.DRAFT) {
            return ApiResponse.error("Exam routine is not published yet");
        }
        routine.setStatus(RoutineStatus.DRAFT);
        routine.setPublishedAt(null);
        routine.setLastModifiedAt(LocalDateTime.now());
        return ApiResponse.success("Exam routine unpublished successfully", examRoutineRepository.save(routine));
    }

    @Override
    public ApiResponse<Void> delete(Integer id) {
        ExamRoutine routine = examRoutineRepository.findById(id).orElse(null);
        if (routine == null) return ApiResponse.error("Exam routine not found");

        if (!routine.getIsActive()) return ApiResponse.error("Exam routine is already inactive");

        if (routine.getStatus() == RoutineStatus.PUBLISHED) {
            return ApiResponse.error("Cannot deactivate a published routine. Unpublish it first");
        }
        routine.setIsActive(false);
        examRoutineRepository.save(routine);
        return ApiResponse.success("Exam routine deleted successfully", null);
    }

    @Override
    public ApiResponse<ExamRoutine> clone(Integer sourceId, CloneRoutineRequestDto dto) {
        ExamRoutine source = examRoutineRepository.findById(sourceId).orElse(null);
        if (source == null) return ApiResponse.error("Source routine not found");

        AcademicYear academicYear = academicYearRepository.findById(dto.getAcademicYearId()).orElse(null);
        if (academicYear == null) return ApiResponse.error("Academic year not found");

        String title = dto.getTitle() == null ? null : dto.getTitle().trim();
        if (title == null || title.isBlank()) {
            return ApiResponse.error("Routine title is required");
        }
        if (examRoutineRepository.existsByTitleAndAcademicYearIdAndIsActiveTrue(
                title, dto.getAcademicYearId())) {
            return ApiResponse.error("An active routine with this title already exists for this academic year");
        }

        ExamRoutine newRoutine = new ExamRoutine();
        newRoutine.setTitle(title);
        newRoutine.setExamType(source.getExamType());
        newRoutine.setAcademicYear(academicYear);
        examRoutineRepository.save(newRoutine);

        List<ExamSession> sourceSessions = examSessionRepository.findByExamRoutineIdAndIsActiveTrue(sourceId);
        for (ExamSession s : sourceSessions) {
            ExamSession copy = new ExamSession();
            copy.setExamRoutine(newRoutine);
            copy.setExamClass(s.getExamClass());
            copy.setSubject(s.getSubject());
            copy.setGroup(s.getGroup());
            copy.setShowOnAdmitCard(s.getShowOnAdmitCard());
            // date and times are intentionally left null — admin assigns them for the new period
            examSessionRepository.save(copy);
        }

        return ApiResponse.success("Routine cloned successfully", newRoutine);
    }

    @Override
    public ApiResponse<ExamRoutine> publishResults(Integer routineId, Integer classId) {
        ExamRoutine routine = examRoutineRepository.findById(routineId).orElse(null);
        if (routine == null) return ApiResponse.error("Exam routine not found");

        List<com.example.academic_service.entity.Class> classes = examSessionRepository.findDistinctClassesByRoutineId(routineId);
        com.example.academic_service.entity.Class targetClass = classes.stream().filter(c -> c.getId().equals(classId)).findFirst().orElse(null);
        if (targetClass == null) return ApiResponse.error("Class not found in this routine");

        boolean published = resultPublicationRepository.findByExamRoutine_IdAndStudentClass_Id(routineId, classId)
                .map(p -> Boolean.TRUE.equals(p.getPublished())).orElse(false);
        if (published)
            return ApiResponse.error("Results already published for this class");
        if (resultPublishService.hasActiveJob(routineId, classId))
            return ApiResponse.error("Results are already being published or checked for this class");
        if (routine.getAcademicYear() == null)
            return ApiResponse.error("Exam routine has no academic year");

        // Computes and stores every student's result in the background, one class at a time;
        // the class becomes visible to students once all its results are stored.
        resultPublishService.enqueue(routineId, classId);
        auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(),
            AuditActionType.UPDATE, Submodule.EXAM_ROUTINES, "ExamRoutine", routineId.toString(),
            "Published results for routine: " + routine.getTitle() + ", class: " + targetClass.getName());
        return ApiResponse.success("Publishing results for " + targetClass.getName(), routine);
    }

    @Override
    public ApiResponse<ExamRoutine> unpublishResults(Integer routineId, Integer classId) {
        ExamRoutine routine = examRoutineRepository.findById(routineId).orElse(null);
        if (routine == null) return ApiResponse.error("Exam routine not found");

        ResultPublication pub = resultPublicationRepository
                .findByExamRoutine_IdAndStudentClass_Id(routineId, classId).orElse(null);
        if (pub == null || !Boolean.TRUE.equals(pub.getPublished()))
            return ApiResponse.error("Results are not published for this class");

        if (resultPublishService.hasActiveJob(routineId, classId))
            return ApiResponse.error("Results are still being published or checked for this class");

        // Also deletes the stored results; publishing again works them out from the current marks.
        resultPublishService.unpublish(routineId, classId);
        auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(),
            AuditActionType.UPDATE, Submodule.EXAM_ROUTINES, "ExamRoutine", routineId.toString(),
            "Unpublished results for routine: " + routine.getTitle() + ", class: " + pub.getStudentClass().getName());
        return ApiResponse.success("Results hidden for " + pub.getStudentClass().getName(), routine);
    }

    @Override
    public ApiResponse<List<Map<String, Object>>> getClassPublicationStatus(Integer routineId) {
        List<com.example.academic_service.entity.Class> classes = examSessionRepository.findDistinctClassesByRoutineId(routineId);
        List<ResultPublication> pubs = resultPublicationRepository.findByExamRoutine_Id(routineId);
        Map<Integer, ResultPublication> pubByClassId = pubs.stream()
                .collect(Collectors.toMap(p -> p.getStudentClass().getId(), p -> p));

        Map<Integer, Map<String, Map<String, Object>>> jobs = resultPublishService.latestJobs(routineId);

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (com.example.academic_service.entity.Class c : classes) {
            ResultPublication pub = pubByClassId.get(c.getId());
            Map<String, Map<String, Object>> classJobs = jobs.getOrDefault(c.getId(), Map.of());
            Map<String, Object> job = classJobs.get(ResultPublishService.PUBLISH);
            Map<String, Object> check = classJobs.get(ResultPublishService.CHECK);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("classId", c.getId());
            m.put("className", c.getName());
            m.put("published", pub != null && Boolean.TRUE.equals(pub.getPublished()));
            m.put("publishedAt", pub != null ? pub.getPublishedAt() : null);
            // Latest publish job: QUEUED / PROCESSING / DONE / FAILED, with progress and any error.
            m.put("publishStatus", job != null ? job.get("status") : null);
            m.put("publishDone", job != null ? job.get("done") : null);
            m.put("publishTotal", job != null ? job.get("total") : null);
            m.put("publishError", job != null ? job.get("error") : null);
            // Latest update check: QUEUED / PROCESSING / DONE (changes waiting for review) / FAILED / APPLIED / DISCARDED.
            m.put("checkJobId", check != null ? check.get("id") : null);
            m.put("checkStatus", check != null ? check.get("status") : null);
            m.put("checkDone", check != null ? check.get("done") : null);
            m.put("checkTotal", check != null ? check.get("total") : null);
            m.put("checkError", check != null ? check.get("error") : null);
            m.put("checkChanged", check != null ? check.get("changed") : null);
            m.put("checkAdded", check != null ? check.get("added") : null);
            m.put("checkRemoved", check != null ? check.get("removed") : null);
            result.add(m);
        }

        return ApiResponse.success("Class publication status fetched", result);
    }

    @Override
    public ApiResponse<Map<String, Object>> checkResultChanges(Integer routineId, Integer classId) {
        ExamRoutine routine = examRoutineRepository.findById(routineId).orElse(null);
        if (routine == null) throw new RuntimeException("Exam routine not found");
        ResultPublication pub = resultPublicationRepository
                .findByExamRoutine_IdAndStudentClass_Id(routineId, classId).orElse(null);
        if (pub == null || !Boolean.TRUE.equals(pub.getPublished()))
            throw new RuntimeException("Results are not published for this class");
        if (resultPublishService.hasActiveJob(routineId, classId))
            throw new RuntimeException("Results are already being published or checked for this class");

        // Recomputes every result in the background and saves only the differences for review.
        long jobId = resultPublishService.enqueueCheck(routineId, classId);
        return ApiResponse.success("Checking results for " + pub.getStudentClass().getName(), Map.of("jobId", jobId));
    }

    @Override
    public ApiResponse<Map<String, Object>> getResultChanges(Integer routineId, Long jobId, int page, int size) {
        return ApiResponse.success("Result changes fetched", resultPublishService.changes(routineId, jobId, page, size));
    }

    @Override
    public ApiResponse<Map<String, Object>> applyResultChanges(Integer routineId, Long jobId) {
        ExamRoutine routine = examRoutineRepository.findById(routineId).orElse(null);
        if (routine == null) throw new RuntimeException("Exam routine not found");
        int[] c = resultPublishService.apply(routineId, jobId);
        auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(),
            AuditActionType.UPDATE, Submodule.EXAM_ROUTINES, "ExamRoutine", routineId.toString(),
            "Updated published results for routine: " + routine.getTitle() + " (check " + jobId + "): "
                + c[0] + " changed, " + c[1] + " added, " + c[2] + " removed");
        return ApiResponse.success("Results updated", Map.of("changed", c[0], "added", c[1], "removed", c[2]));
    }

    @Override
    public ApiResponse<Void> discardResultChanges(Integer routineId, Long jobId) {
        resultPublishService.discard(routineId, jobId);
        return ApiResponse.success("Changes discarded", null);
    }
}
