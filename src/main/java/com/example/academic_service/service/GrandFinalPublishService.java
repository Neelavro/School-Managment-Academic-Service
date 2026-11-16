package com.example.academic_service.service;

import com.example.academic_service.entity.AcademicYear;
import com.example.academic_service.entity.AuditActionType;
import com.example.academic_service.entity.ExamRoutine;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.repository.AcademicYearRepository;
import com.example.academic_service.repository.ExamRoutineRepository;
import com.example.academic_service.repository.ExamSessionRepository;
import com.example.academic_service.util.AuditHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;

/**
 * Publishing the Grand Final (a class's annual result) to the student portal, class by class, like an exam's
 * results: Release (a background job stores every student's result and transcript page), Hide (deletes them),
 * Update (check → review → apply). The storage and jobs are ResultPublishService's, under grandFinalKey(year).
 */
@Service
@RequiredArgsConstructor
public class GrandFinalPublishService {

    private final ResultPublishService resultPublishService;
    private final AcademicYearRepository academicYearRepository;
    private final ExamRoutineRepository examRoutineRepository;
    private final ExamSessionRepository examSessionRepository;
    private final AuditLogService auditLogService;

    private AcademicYear year(Integer academicYearId) {
        return academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new IllegalArgumentException("Academic year not found"));
    }

    /** Classes with papers in the year's exams that count for the annual result, in the school's class order. */
    private List<com.example.academic_service.entity.Class> classes(Integer academicYearId) {
        Map<Integer, com.example.academic_service.entity.Class> byId = new LinkedHashMap<>();
        for (ExamRoutine r : examRoutineRepository.findByAcademicYearIdAndIsActiveTrue(academicYearId)) {
            if (Boolean.FALSE.equals(r.getConsiderForAnnualResult())) continue;
            for (com.example.academic_service.entity.Class c : examSessionRepository.findDistinctClassesByRoutineId(r.getId()))
                byId.putIfAbsent(c.getId(), c);
        }
        List<com.example.academic_service.entity.Class> list = new ArrayList<>(byId.values());
        list.sort(Comparator.comparing((com.example.academic_service.entity.Class c) -> c.getOrderIndex() == null ? Integer.MAX_VALUE : c.getOrderIndex())
                .thenComparing(c -> c.getName() == null ? "" : c.getName()));
        return list;
    }

    private com.example.academic_service.entity.Class requireClass(Integer academicYearId, Integer classId) {
        return classes(academicYearId).stream().filter(c -> c.getId().equals(classId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("This class has no exams counted for the annual result in this year"));
    }

    /** Same fields as an exam's class-publication-status, one row per class. */
    public List<Map<String, Object>> status(Integer academicYearId) {
        year(academicYearId);
        int key = ResultPublishService.grandFinalKey(academicYearId);
        Map<Integer, Timestamp> published = resultPublishService.publishedGrandFinals(academicYearId);
        Map<Integer, Map<String, Map<String, Object>>> jobs = resultPublishService.latestJobs(key);
        List<Map<String, Object>> out = new ArrayList<>();
        for (com.example.academic_service.entity.Class c : classes(academicYearId)) {
            Map<String, Map<String, Object>> classJobs = jobs.getOrDefault(c.getId(), Map.of());
            Map<String, Object> job = classJobs.get(ResultPublishService.PUBLISH);
            Map<String, Object> check = classJobs.get(ResultPublishService.CHECK);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("classId", c.getId());
            m.put("className", c.getName());
            m.put("published", published.containsKey(c.getId()));
            m.put("publishedAt", published.get(c.getId()));
            m.put("publishStatus", job != null ? job.get("status") : null);
            m.put("publishDone", job != null ? job.get("done") : null);
            m.put("publishTotal", job != null ? job.get("total") : null);
            m.put("publishError", job != null ? job.get("error") : null);
            m.put("checkJobId", check != null ? check.get("id") : null);
            m.put("checkStatus", check != null ? check.get("status") : null);
            m.put("checkDone", check != null ? check.get("done") : null);
            m.put("checkTotal", check != null ? check.get("total") : null);
            m.put("checkError", check != null ? check.get("error") : null);
            m.put("checkChanged", check != null ? check.get("changed") : null);
            m.put("checkAdded", check != null ? check.get("added") : null);
            m.put("checkRemoved", check != null ? check.get("removed") : null);
            out.add(m);
        }
        return out;
    }

    public String publish(Integer academicYearId, Integer classId) {
        AcademicYear y = year(academicYearId);
        com.example.academic_service.entity.Class c = requireClass(academicYearId, classId);
        int key = ResultPublishService.grandFinalKey(academicYearId);
        if (resultPublishService.publishedGrandFinals(academicYearId).containsKey(classId))
            throw new IllegalArgumentException("The Grand Final is already published for this class");
        if (resultPublishService.hasActiveJob(key, classId))
            throw new IllegalArgumentException("The Grand Final is already being published or checked for this class");
        resultPublishService.enqueue(key, classId);
        audit("Published Grand Final " + y.getYearName() + ", class: " + c.getName(), academicYearId);
        return "Publishing the Grand Final for " + c.getName();
    }

    public String unpublish(Integer academicYearId, Integer classId) {
        AcademicYear y = year(academicYearId);
        int key = ResultPublishService.grandFinalKey(academicYearId);
        if (!resultPublishService.publishedGrandFinals(academicYearId).containsKey(classId))
            throw new IllegalArgumentException("The Grand Final is not published for this class");
        if (resultPublishService.hasActiveJob(key, classId))
            throw new IllegalArgumentException("The Grand Final is still being published or checked for this class");
        resultPublishService.unpublish(key, classId);
        String name = classes(academicYearId).stream().filter(c -> c.getId().equals(classId))
                .map(com.example.academic_service.entity.Class::getName).findFirst().orElse("class " + classId);
        audit("Hid Grand Final " + y.getYearName() + ", class: " + name, academicYearId);
        return "Grand Final hidden for " + name;
    }

    public long check(Integer academicYearId, Integer classId) {
        year(academicYearId);
        int key = ResultPublishService.grandFinalKey(academicYearId);
        if (!resultPublishService.publishedGrandFinals(academicYearId).containsKey(classId))
            throw new IllegalArgumentException("The Grand Final is not published for this class");
        if (resultPublishService.hasActiveJob(key, classId))
            throw new IllegalArgumentException("The Grand Final is already being published or checked for this class");
        return resultPublishService.enqueueCheck(key, classId);
    }

    public Map<String, Object> changes(Integer academicYearId, Long jobId, int page, int size) {
        return resultPublishService.changes(ResultPublishService.grandFinalKey(academicYearId), jobId, page, size);
    }

    public int[] apply(Integer academicYearId, Long jobId) {
        AcademicYear y = year(academicYearId);
        int[] c = resultPublishService.apply(ResultPublishService.grandFinalKey(academicYearId), jobId);
        audit("Updated published Grand Final " + y.getYearName() + " (check " + jobId + "): "
                + c[0] + " changed, " + c[1] + " added, " + c[2] + " removed", academicYearId);
        return c;
    }

    public void discard(Integer academicYearId, Long jobId) {
        resultPublishService.discard(ResultPublishService.grandFinalKey(academicYearId), jobId);
    }

    private void audit(String text, Integer academicYearId) {
        auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(), AuditActionType.UPDATE,
                Submodule.EXAM_ROUTINES, "GrandFinal", academicYearId.toString(), text);
    }
}
