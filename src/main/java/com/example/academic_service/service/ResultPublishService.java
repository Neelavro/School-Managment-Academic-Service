package com.example.academic_service.service;

import com.example.academic_service.dto.result_dtos.ProgressReportData;
import com.example.academic_service.dto.result_dtos.StudentRoutineResultResponse;
import com.example.academic_service.entity.Enrollment;
import com.example.academic_service.entity.ExamRoutine;
import com.example.academic_service.entity.Grade;
import com.example.academic_service.entity.GradingPolicy;
import com.example.academic_service.repository.EnrollmentRepository;
import com.example.academic_service.repository.ExamRoutineRepository;
import com.example.academic_service.repository.GradingPolicyRepository;
import tools.jackson.databind.json.JsonMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Stored results for the student portal.
 *
 * Publishing a class queues a PUBLISH job. One worker thread processes jobs one class at a time:
 * it computes every active student's result, then in one transaction replaces the class's
 * published_result rows and marks the class published, so students never see half a class.
 * Unpublishing deletes the rows.
 *
 * Stored results are a snapshot: marks and marking structures stay editable, and students keep
 * seeing the stored result until the class is updated. To update a published class without taking
 * it down, a CHECK job recomputes every result and saves only the differences as a preview
 * (result_update_change). The admin reviews it, then applies it (the class stays live and only the
 * changed rows are written) or discards it.
 *
 * Each stored row also carries the student's progress-report data (report_json), built with the same
 * code as the admin progress-report PDF: highest marks, positions, merged-subject grades and the grading
 * table need the whole class, so they're computed here once and the student portal draws the PDF from them.
 * A check recomputes them for the whole class (one changed mark can move everyone's position) and applying
 * it replaces them. REPORT jobs fill them in for classes published before report_json existed.
 */
@Service
@RequiredArgsConstructor
public class ResultPublishService {

    private static final Logger log = LoggerFactory.getLogger(ResultPublishService.class);

    // Job kinds
    public static final String PUBLISH = "PUBLISH";
    public static final String CHECK = "CHECK";
    public static final String REPORT = "REPORT";

    // Job statuses. A CHECK job that is DONE holds a preview waiting for apply or discard.
    public static final String QUEUED = "QUEUED";
    public static final String PROCESSING = "PROCESSING";
    public static final String DONE = "DONE";
    public static final String FAILED = "FAILED";
    public static final String APPLIED = "APPLIED";
    public static final String DISCARDED = "DISCARDED";

    // Change types in a CHECK preview
    public static final String CHANGED = "CHANGED";
    public static final String ADDED = "ADDED";
    public static final String REMOVED = "REMOVED";

    private static final int CHUNK = 25;

    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager transactionManager;
    private final ResultService resultService;
    private final EnrollmentRepository enrollmentRepository;
    private final ExamRoutineRepository examRoutineRepository;
    private final GradingPolicyRepository gradingPolicyRepository;
    private final JsonMapper objectMapper; // Spring's own HTTP mapper, so stored JSON matches what the API used to send

    // One thread: classes are processed one at a time so a "publish all" doesn't swamp the CPU.
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "result-publish");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean stopping;

    @PreDestroy
    void shutdown() {
        stopping = true;
        worker.shutdownNow();
    }

    // ── Queue ────────────────────────────────────────────────────────────────

    /** True while a publish or check job for the class is queued or running. */
    public boolean hasActiveJob(Integer routineId, Integer classId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM result_publish_job WHERE routine_id = ? AND class_id = ? AND status IN (?, ?)",
                Integer.class, routineId, classId, QUEUED, PROCESSING);
        return n != null && n > 0;
    }

    /** Queues a publish job for one class and returns its id. Any pending update preview is dropped. */
    public long enqueue(Integer routineId, Integer classId) {
        discardPreviews(routineId, classId);
        return enqueue(routineId, classId, PUBLISH);
    }

    /** Queues a CHECK job for a published class: recompute and save the differences for review. */
    public long enqueueCheck(Integer routineId, Integer classId) {
        discardPreviews(routineId, classId);
        return enqueue(routineId, classId, CHECK);
    }

    private long enqueue(Integer routineId, Integer classId, String kind) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO result_publish_job (routine_id, class_id, kind, status, done, created_at) VALUES (?, ?, ?, ?, 0, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, routineId);
            ps.setInt(2, classId);
            ps.setString(3, kind);
            ps.setString(4, QUEUED);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, key);
        long id = key.getKey().longValue();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // Called inside a transaction: the worker must not look for the job before it's committed.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    worker.submit(() -> process(id));
                }
            });
        } else {
            worker.submit(() -> process(id));
        }
        return id;
    }

    /**
     * On startup: finish jobs interrupted by a restart, and create the stored rows for classes
     * that were published before published_result existed (or whose rows are missing).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void resumeOnStartup() {
        try {
            jdbc.update("UPDATE result_publish_job SET status = ? WHERE status = ?", QUEUED, PROCESSING);
            List<Map<String, Object>> missing = jdbc.queryForList("""
                    SELECT rp.routine_id, rp.class_id FROM result_publication rp
                    WHERE rp.published = 1
                      AND NOT EXISTS (SELECT 1 FROM published_result pr
                                      WHERE pr.routine_id = rp.routine_id AND pr.class_id = rp.class_id)
                      AND NOT EXISTS (SELECT 1 FROM result_publish_job j
                                      WHERE j.routine_id = rp.routine_id AND j.class_id = rp.class_id AND j.status = ?)
                    """, QUEUED);
            for (Map<String, Object> m : missing) {
                jdbc.update("INSERT INTO result_publish_job (routine_id, class_id, kind, status, done, created_at) VALUES (?, ?, ?, ?, 0, ?)",
                        m.get("routine_id"), m.get("class_id"), PUBLISH, QUEUED, Timestamp.valueOf(LocalDateTime.now()));
            }
            // Published classes whose stored rows have no progress-report data yet.
            List<Map<String, Object>> noReports = jdbc.queryForList("""
                    SELECT DISTINCT pr.routine_id, pr.class_id FROM published_result pr
                    WHERE pr.report_json IS NULL
                      AND NOT EXISTS (SELECT 1 FROM result_publish_job j
                                      WHERE j.routine_id = pr.routine_id AND j.class_id = pr.class_id AND j.status = ?)
                    """, QUEUED);
            for (Map<String, Object> m : noReports) {
                jdbc.update("INSERT INTO result_publish_job (routine_id, class_id, kind, status, done, created_at) VALUES (?, ?, ?, ?, 0, ?)",
                        m.get("routine_id"), m.get("class_id"), REPORT, QUEUED, Timestamp.valueOf(LocalDateTime.now()));
            }
            List<Long> queued = jdbc.queryForList(
                    "SELECT id FROM result_publish_job WHERE status = ? ORDER BY id", Long.class, QUEUED);
            queued.forEach(id -> worker.submit(() -> process(id)));
            if (!queued.isEmpty())
                log.info("Result publish: {} job(s) queued on startup ({} backfill, {} report backfill)",
                        queued.size(), missing.size(), noReports.size());
        } catch (Exception e) {
            // Most likely the migration hasn't been run yet; the portal would fail loudly anyway.
            log.error("Result publish: startup resume failed — is sql/2026_09_27__published_results.sql applied? {}",
                    e.getMessage());
        }
    }

    // ── Worker ───────────────────────────────────────────────────────────────

    /** One student's freshly computed result, ready to store. */
    private record Computed(Long enrollmentId, String studentSystemId, BigDecimal totalMarks, Double gpa,
                            boolean passed, String json) {}

    private void process(Long jobId) {
        // Claim the job; skip it if another submit already took it.
        if (jdbc.update("UPDATE result_publish_job SET status = ?, started_at = ? WHERE id = ? AND status = ?",
                PROCESSING, Timestamp.valueOf(LocalDateTime.now()), jobId, QUEUED) == 0) return;

        Map<String, Object> job = jdbc.queryForMap("SELECT routine_id, class_id, kind FROM result_publish_job WHERE id = ?", jobId);
        Integer routineId = ((Number) job.get("routine_id")).intValue();
        Integer classId = ((Number) job.get("class_id")).intValue();
        String kind = (String) job.get("kind");
        long started = System.currentTimeMillis();
        try {
            if (REPORT.equals(kind)) {
                Map<Long, String> reports = computeReports(routineId, classId);
                jdbc.batchUpdate("UPDATE published_result SET report_json = ? WHERE routine_id = ? AND class_id = ? AND enrollment_id = ?",
                        reports.entrySet().stream().map(e -> new Object[]{e.getValue(), routineId, classId, e.getKey()}).toList());
                jdbc.update("UPDATE result_publish_job SET status = ?, total = ?, done = ?, finished_at = ? WHERE id = ?",
                        DONE, reports.size(), reports.size(), Timestamp.valueOf(LocalDateTime.now()), jobId);
                log.info("Result reports: routine={} class={} stored {} progress report(s) in {} ms",
                        routineId, classId, reports.size(), System.currentTimeMillis() - started);
                return;
            }

            List<String> skipped = new ArrayList<>();
            List<Computed> results = computeClass(jobId, routineId, classId, skipped);
            Map<Long, String> reports = computeReports(routineId, classId);
            String note = skipped.isEmpty() ? null : truncate(skipped.size() + " student(s) skipped. First: " + skipped.get(0));

            if (CHECK.equals(kind)) {
                int[] counts = saveDiff(jobId, routineId, classId, results, skipped);
                jdbc.batchUpdate("INSERT INTO result_update_report (job_id, enrollment_id, report_json) VALUES (?, ?, ?)",
                        reports.entrySet().stream().map(e -> new Object[]{jobId, e.getKey(), e.getValue()}).toList());
                jdbc.update("""
                        UPDATE result_publish_job SET status = ?, done = ?, error = ?, changed = ?, added = ?, removed = ?, finished_at = ?
                        WHERE id = ?
                        """, DONE, results.size() + skipped.size(), note, counts[0], counts[1], counts[2],
                        Timestamp.valueOf(LocalDateTime.now()), jobId);
                log.info("Result check: routine={} class={} changed {}, added {}, removed {}, skipped {} in {} ms",
                        routineId, classId, counts[0], counts[1], counts[2], skipped.size(), System.currentTimeMillis() - started);
                return;
            }

            storeClass(routineId, classId, results, reports);
            jdbc.update("UPDATE result_publish_job SET status = ?, done = ?, error = ?, finished_at = ? WHERE id = ?",
                    DONE, results.size() + skipped.size(), note, Timestamp.valueOf(LocalDateTime.now()), jobId);
            log.info("Result publish: routine={} class={} stored {} result(s), skipped {} in {} ms",
                    routineId, classId, results.size(), skipped.size(), System.currentTimeMillis() - started);
        } catch (Exception e) {
            if (stopping) {
                // Interrupted by shutdown: leave it PROCESSING so the next startup runs it again.
                log.warn("Result job interrupted by shutdown: job={} routine={} class={}", jobId, routineId, classId);
                return;
            }
            log.error("Result job failed: job={} kind={} routine={} class={}", jobId, kind, routineId, classId, e);
            jdbc.update("UPDATE result_publish_job SET status = ?, error = ?, finished_at = ? WHERE id = ?",
                    FAILED, truncate(e.getMessage() != null ? e.getMessage() : e.toString()),
                    Timestamp.valueOf(LocalDateTime.now()), jobId);
        }
    }

    /** Computes every active student's result in the class, reporting progress on the job row. */
    private List<Computed> computeClass(Long jobId, Integer routineId, Integer classId, List<String> skipped) {
        ExamRoutine routine = examRoutineRepository.findById(routineId)
                .orElseThrow(() -> new IllegalStateException("Exam routine not found: " + routineId));
        if (routine.getAcademicYear() == null)
            throw new IllegalStateException("Exam routine has no academic year");
        Integer academicYearId = routine.getAcademicYear().getId();

        List<Enrollment> enrollments = enrollmentRepository
                .findByStudentClass_IdAndAcademicYear_IdAndIsActiveTrue(classId, academicYearId);
        jdbc.update("UPDATE result_publish_job SET total = ? WHERE id = ?", enrollments.size(), jobId);

        List<Computed> results = new ArrayList<>();
        TransactionTemplate readTx = new TransactionTemplate(transactionManager);
        readTx.setReadOnly(true);
        // Chunks share one read-only transaction, so reference data (subjects, structures,
        // grades) is loaded once per chunk instead of once per repository call. Progress is
        // written between chunks, outside the read-only transaction.
        for (int from = 0; from < enrollments.size(); from += CHUNK) {
            List<Enrollment> chunk = enrollments.subList(from, Math.min(from + CHUNK, enrollments.size()));
            readTx.executeWithoutResult(status -> {
                for (Enrollment e : chunk) {
                    try {
                        StudentRoutineResultResponse r = resultService.getStudentRoutineResult(e.getId(), routineId);
                        results.add(new Computed(e.getId(), e.getStudentSystemId(), r.getTotalMarks(),
                                r.getOverallGpa(), r.isPassed(), objectMapper.writeValueAsString(r)));
                    } catch (Exception ex) {
                        skipped.add(e.getStudentSystemId() + ": " + ex.getMessage());
                    }
                }
            });
            jdbc.update("UPDATE result_publish_job SET done = ? WHERE id = ?", results.size() + skipped.size(), jobId);
        }
        return results;
    }

    /** Replaces the class's stored rows and marks it published, in one transaction. */
    private void storeClass(Integer routineId, Integer classId, List<Computed> results, Map<Long, String> reports) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        List<Object[]> rows = results.stream()
                .map(c -> new Object[]{routineId, classId, c.enrollmentId(), c.studentSystemId(),
                        c.totalMarks(), c.gpa(), c.passed(), c.json(), reports.get(c.enrollmentId()), now})
                .toList();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("DELETE FROM published_result WHERE routine_id = ? AND class_id = ?", routineId, classId);
            jdbc.batchUpdate("""
                    INSERT INTO published_result (routine_id, class_id, enrollment_id, student_system_id,
                                                  total_marks, gpa, passed, result_json, report_json, computed_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, rows);
            int updated = jdbc.update("""
                    UPDATE result_publication SET published = 1, published_at = COALESCE(published_at, ?)
                    WHERE routine_id = ? AND class_id = ?
                    """, now, routineId, classId);
            if (updated == 0)
                jdbc.update("INSERT INTO result_publication (routine_id, class_id, published, published_at) VALUES (?, ?, 1, ?)",
                        routineId, classId, now);
        });
    }

    private static String truncate(String s) {
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }

    /**
     * Each student's progress-report data (enrollmentId → JSON), from the same code as the admin
     * progress-report PDF, plus the grading table it prints. Student details (name, phones, photo)
     * are left to the portal, which reads them fresh from the profile. A failure here doesn't stop
     * publishing: the portal then says the report isn't available.
     */
    private Map<Long, String> computeReports(Integer routineId, Integer classId) {
        Map<Long, String> reports = new HashMap<>();
        try {
            TransactionTemplate readTx = new TransactionTemplate(transactionManager);
            readTx.setReadOnly(true);
            readTx.executeWithoutResult(status -> {
                List<Map<String, Object>> grades = gradingTable();
                for (ProgressReportData part : resultService.getProgressReportDataPartitioned(
                        routineId, classId, null, null, null, null, null, null)) {
                    for (ProgressReportData.StudentReport student : part.getStudents()) {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("routineTitle", part.getRoutineTitle());
                        r.put("examTypeName", part.getExamTypeName());
                        r.put("className", part.getClassName());
                        r.put("academicYearName", part.getAcademicYearName());
                        r.put("useGpaForResult", part.isUseGpaForResult());
                        r.put("components", part.getComponents());
                        r.put("subjects", part.getSubjects());
                        r.put("student", student);
                        r.put("grades", grades);
                        reports.put(student.getEnrollmentId(), objectMapper.writeValueAsString(r));
                    }
                }
            });
        } catch (Exception e) {
            log.error("Progress report data failed: routine={} class={}", routineId, classId, e);
        }
        return reports;
    }

    /** The grading table the progress report prints: the first active policy, highest grade first. */
    private List<Map<String, Object>> gradingTable() {
        List<GradingPolicy> active = gradingPolicyRepository.findByIsActive(true);
        if (active.isEmpty()) return List.of();
        List<Grade> grades = new ArrayList<>(active.get(0).getGrades());
        grades.sort(Comparator.comparingDouble(Grade::getMinMark).reversed());
        List<Map<String, Object>> out = new ArrayList<>();
        for (Grade g : grades) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", g.getName());
            m.put("minMark", g.getMinMark());
            m.put("maxMark", g.getMaxMark());
            m.put("gpaValue", g.getGpaValue());
            m.put("comment", g.getComment());
            out.add(m);
        }
        return out;
    }

    // ── Update: check, review, apply ─────────────────────────────────────────

    /**
     * Compares fresh results with the stored ones and saves only the differences for review.
     * Students whose result couldn't be computed are left as they are (never counted as removed).
     * Returns {changed, added, removed}.
     */
    private int[] saveDiff(Long jobId, Integer routineId, Integer classId, List<Computed> results, List<String> skipped) {
        Map<Long, String> stored = new HashMap<>();
        Map<Long, String> storedSid = new HashMap<>();
        jdbc.query("SELECT enrollment_id, student_system_id, result_json FROM published_result WHERE routine_id = ? AND class_id = ?",
                rs -> {
                    stored.put(rs.getLong(1), rs.getString(3));
                    storedSid.put(rs.getLong(1), rs.getString(2));
                }, routineId, classId);
        Set<String> skippedSids = new HashSet<>();
        skipped.forEach(s -> skippedSids.add(s.substring(0, s.indexOf(':'))));

        List<Object[]> rows = new ArrayList<>();
        int[] counts = new int[3];
        Set<Long> seen = new HashSet<>();
        for (Computed c : results) {
            seen.add(c.enrollmentId());
            String old = stored.get(c.enrollmentId());
            if (c.json().equals(old)) continue;
            Map<String, Object> next = readMap(c.json());
            String type = old == null ? ADDED : CHANGED;
            Map<String, Object> diff = old == null ? summary(next) : diff(readMap(old), next);
            rows.add(changeRow(jobId, c.enrollmentId(), c.studentSystemId(), next, type, diff, c));
            counts[old == null ? 1 : 0]++;
        }
        for (Map.Entry<Long, String> e : stored.entrySet()) {
            if (seen.contains(e.getKey()) || skippedSids.contains(storedSid.get(e.getKey()))) continue;
            Map<String, Object> prev = readMap(e.getValue());
            rows.add(changeRow(jobId, e.getKey(), storedSid.get(e.getKey()), prev, REMOVED, summary(prev), null));
            counts[2]++;
        }
        jdbc.batchUpdate("""
                INSERT INTO result_update_change (job_id, enrollment_id, student_system_id, student_name, class_roll,
                                                  change_type, diff_json, total_marks, gpa, passed, result_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, rows);
        return counts;
    }

    private Object[] changeRow(Long jobId, Long enrollmentId, String sid, Map<String, Object> result,
                               String type, Map<String, Object> diff, Computed next) {
        Object roll = result.get("classRoll");
        return new Object[]{jobId, enrollmentId, sid, result.get("studentName"),
                roll instanceof Number n ? n.intValue() : null, type, objectMapper.writeValueAsString(diff),
                next != null ? next.totalMarks() : null, next != null ? next.gpa() : null,
                next != null ? next.passed() : null, next != null ? next.json() : null};
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String json) {
        return objectMapper.readValue(json, LinkedHashMap.class);
    }

    private static final List<String> SUMMARY_FIELDS = List.of("totalMarks", "overallGpa", "passed");

    /** The headline numbers of a result, for an added or removed student. */
    private static Map<String, Object> summary(Map<String, Object> r) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (String f : SUMMARY_FIELDS) m.put(f, r.get(f));
        return Map.of("summary", m);
    }

    /**
     * What differs between a stored and a fresh result:
     * fields → [{field, old, new}] for top-level values (total, GPA, pass, name, roll…),
     * subjects → [{subjectName, old, new}] with the whole subject entry on each side (null if absent).
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> diff(Map<String, Object> prev, Map<String, Object> next) {
        List<Map<String, Object>> fields = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>(prev.keySet());
        keys.addAll(next.keySet());
        keys.remove("subjectResults");
        for (String k : keys) {
            if (!Objects.equals(prev.get(k), next.get(k))) {
                Map<String, Object> f = new LinkedHashMap<>();
                f.put("field", k);
                f.put("old", prev.get(k));
                f.put("new", next.get(k));
                fields.add(f);
            }
        }

        Map<Object, Map<String, Object>> before = bySubject((List<Map<String, Object>>) prev.get("subjectResults"));
        Map<Object, Map<String, Object>> after = bySubject((List<Map<String, Object>>) next.get("subjectResults"));
        Set<Object> ids = new LinkedHashSet<>(after.keySet());
        ids.addAll(before.keySet());
        List<Map<String, Object>> subjects = new ArrayList<>();
        for (Object id : ids) {
            Map<String, Object> a = before.get(id), b = after.get(id);
            if (Objects.equals(a, b)) continue;
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("subjectName", (b != null ? b : a).get("subjectName"));
            s.put("old", a);
            s.put("new", b);
            subjects.add(s);
        }

        Map<String, Object> d = new LinkedHashMap<>();
        Map<String, Object> m = new LinkedHashMap<>();
        for (String f : SUMMARY_FIELDS) m.put(f, next.get(f));
        d.put("summary", m);
        d.put("fields", fields);
        d.put("subjects", subjects);
        return d;
    }

    private static Map<Object, Map<String, Object>> bySubject(List<Map<String, Object>> list) {
        Map<Object, Map<String, Object>> m = new LinkedHashMap<>();
        if (list != null)
            for (Map<String, Object> s : list)
                m.put(s.get("subjectId") != null ? s.get("subjectId") : s.get("subjectName"), s);
        return m;
    }

    /** Latest CHECK job with a preview waiting for review, if it belongs to this routine × class. */
    private Map<String, Object> pendingCheck(Long jobId, Integer routineId) {
        List<Map<String, Object>> jobs = jdbc.queryForList(
                "SELECT id, class_id, kind, status FROM result_publish_job WHERE id = ? AND routine_id = ?", jobId, routineId);
        if (jobs.isEmpty() || !CHECK.equals(jobs.get(0).get("kind")))
            throw new RuntimeException("Update check not found");
        if (!DONE.equals(jobs.get(0).get("status")))
            throw new RuntimeException("This update check is no longer pending. Check for changes again.");
        return jobs.get(0);
    }

    /** One page of a check's changes, students in roll order. */
    public Map<String, Object> changes(Integer routineId, Long jobId, int page, int size) {
        pendingCheck(jobId, routineId);
        size = Math.max(1, Math.min(size, 100));
        page = Math.max(0, page);
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM result_update_change WHERE job_id = ?", Integer.class, jobId);
        List<Map<String, Object>> content = jdbc.query("""
                SELECT enrollment_id, student_system_id, student_name, class_roll, change_type, diff_json
                FROM result_update_change WHERE job_id = ?
                ORDER BY class_roll IS NULL, class_roll, student_system_id
                LIMIT ? OFFSET ?
                """, (rs, i) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("enrollmentId", rs.getLong(1));
            m.put("studentSystemId", rs.getString(2));
            m.put("studentName", rs.getString(3));
            m.put("classRoll", rs.getObject(4));
            m.put("changeType", rs.getString(5));
            m.put("diff", readMap(rs.getString(6)));
            return m;
        }, jobId, size, page * size);
        int totalElements = total != null ? total : 0;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("content", content);
        m.put("page", page);
        m.put("size", size);
        m.put("totalElements", totalElements);
        m.put("totalPages", (totalElements + size - 1) / size);
        return m;
    }

    /**
     * Writes a reviewed check to the stored results in one transaction: changed rows are updated,
     * added ones inserted, removed ones deleted. The class stays published throughout.
     * Returns {changed, added, removed}.
     */
    public int[] apply(Integer routineId, Long jobId) {
        Map<String, Object> job = pendingCheck(jobId, routineId);
        Integer classId = ((Number) job.get("class_id")).intValue();
        Integer published = jdbc.queryForObject(
                "SELECT COUNT(*) FROM result_publication WHERE routine_id = ? AND class_id = ? AND published = 1",
                Integer.class, routineId, classId);
        if (published == null || published == 0)
            throw new RuntimeException("Results for this class aren't published, so there's nothing to update.");

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        int[] counts = new int[3];
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            // Claim the preview first, so two clicks can't apply it twice.
            if (jdbc.update("UPDATE result_publish_job SET status = ?, finished_at = ? WHERE id = ? AND status = ?",
                    APPLIED, now, jobId, DONE) == 0)
                throw new RuntimeException("This update check is no longer pending. Check for changes again.");
            counts[0] = jdbc.update("""
                    UPDATE published_result pr JOIN result_update_change c
                      ON c.job_id = ? AND c.change_type = ? AND c.enrollment_id = pr.enrollment_id
                    SET pr.result_json = c.result_json, pr.total_marks = c.total_marks, pr.gpa = c.gpa,
                        pr.passed = c.passed, pr.computed_at = ?
                    WHERE pr.routine_id = ? AND pr.class_id = ?
                    """, jobId, CHANGED, now, routineId, classId);
            // A student moved here from another class keeps their enrollment id: take the row over.
            counts[1] = jdbc.update("""
                    INSERT INTO published_result (routine_id, class_id, enrollment_id, student_system_id,
                                                  total_marks, gpa, passed, result_json, computed_at)
                    SELECT ?, ?, c.enrollment_id, c.student_system_id, c.total_marks, c.gpa, c.passed, c.result_json, ?
                    FROM result_update_change c WHERE c.job_id = ? AND c.change_type = ?
                    ON DUPLICATE KEY UPDATE class_id = VALUES(class_id), student_system_id = VALUES(student_system_id),
                        total_marks = VALUES(total_marks), gpa = VALUES(gpa), passed = VALUES(passed),
                        result_json = VALUES(result_json), computed_at = VALUES(computed_at)
                    """, routineId, classId, now, jobId, ADDED);
            counts[2] = jdbc.update("""
                    DELETE pr FROM published_result pr JOIN result_update_change c
                      ON c.job_id = ? AND c.change_type = ? AND c.enrollment_id = pr.enrollment_id
                    WHERE pr.routine_id = ? AND pr.class_id = ?
                    """, jobId, REMOVED, routineId, classId);
            // Positions and highest marks can move for everyone, so every student's report data is replaced.
            jdbc.update("""
                    UPDATE published_result pr JOIN result_update_report r
                      ON r.job_id = ? AND r.enrollment_id = pr.enrollment_id
                    SET pr.report_json = r.report_json
                    WHERE pr.routine_id = ? AND pr.class_id = ?
                    """, jobId, routineId, classId);
            jdbc.update("DELETE FROM result_update_change WHERE job_id = ?", jobId);
            jdbc.update("DELETE FROM result_update_report WHERE job_id = ?", jobId);
        });
        log.info("Result update applied: routine={} class={} changed {}, added {}, removed {}",
                routineId, classId, counts[0], counts[1], counts[2]);
        return counts;
    }

    /** Drops a check's preview without touching the stored results. */
    public void discard(Integer routineId, Long jobId) {
        pendingCheck(jobId, routineId);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("UPDATE result_publish_job SET status = ?, finished_at = ? WHERE id = ? AND status = ?",
                    DISCARDED, Timestamp.valueOf(LocalDateTime.now()), jobId, DONE);
            jdbc.update("DELETE FROM result_update_change WHERE job_id = ?", jobId);
            jdbc.update("DELETE FROM result_update_report WHERE job_id = ?", jobId);
        });
    }

    /** Publishing or unpublishing a class makes any preview for it meaningless. */
    private void discardPreviews(Integer routineId, Integer classId) {
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM result_publish_job WHERE routine_id = ? AND class_id = ? AND kind = ? AND status = ?",
                Long.class, routineId, classId, CHECK, DONE);
        for (Long id : ids) {
            jdbc.update("UPDATE result_publish_job SET status = ? WHERE id = ?", DISCARDED, id);
            jdbc.update("DELETE FROM result_update_change WHERE job_id = ?", id);
            jdbc.update("DELETE FROM result_update_report WHERE job_id = ?", id);
        }
    }

    // ── Unpublish ────────────────────────────────────────────────────────────

    /** Hides the class's results and deletes the stored rows (and any pending preview), in one transaction. */
    public void unpublish(Integer routineId, Integer classId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("UPDATE result_publication SET published = 0, published_at = NULL WHERE routine_id = ? AND class_id = ?",
                    routineId, classId);
            jdbc.update("DELETE FROM published_result WHERE routine_id = ? AND class_id = ?", routineId, classId);
            discardPreviews(routineId, classId);
        });
    }

    // ── Status (admin publish screen) ────────────────────────────────────────

    /** Latest job of each kind per class for a routine: classId → kind → job row. */
    public Map<Integer, Map<String, Map<String, Object>>> latestJobs(Integer routineId) {
        Map<Integer, Map<String, Map<String, Object>>> byClass = new HashMap<>();
        for (Map<String, Object> j : jdbc.queryForList("""
                SELECT id, class_id, kind, status, done, total, error, changed, added, removed
                FROM result_publish_job WHERE routine_id = ? ORDER BY id DESC
                """, routineId)) {
            byClass.computeIfAbsent(((Number) j.get("class_id")).intValue(), k -> new HashMap<>())
                    .putIfAbsent((String) j.get("kind"), j);
        }
        return byClass;
    }

    // ── Student portal ───────────────────────────────────────────────────────

    /** A stored result and its version (changes whenever the row is rewritten). */
    public record StoredResult(String version, String json) {}

    /**
     * The student's stored result for a routine, or empty if it isn't published.
     * A row only exists while the class is published, so this is also the publication check.
     */
    public Optional<StoredResult> findForStudent(String studentSystemId, Integer routineId) {
        List<StoredResult> rows = jdbc.query("""
                SELECT id, computed_at, result_json FROM published_result
                WHERE student_system_id = ? AND routine_id = ? ORDER BY id DESC LIMIT 1
                """,
                (rs, i) -> new StoredResult(rs.getLong(1) + "-" + rs.getTimestamp(2).getTime(), rs.getString(3)),
                studentSystemId, routineId);
        return rows.stream().findFirst();
    }

    /**
     * The student's stored progress-report data for a routine: empty if the class isn't published;
     * a row with null json while the report data is still being prepared.
     */
    public Optional<StoredResult> findReportForStudent(String studentSystemId, Integer routineId) {
        List<StoredResult> rows = jdbc.query("""
                SELECT id, computed_at, report_json FROM published_result
                WHERE student_system_id = ? AND routine_id = ? ORDER BY id DESC LIMIT 1
                """,
                (rs, i) -> new StoredResult(rs.getLong(1) + "-" + rs.getTimestamp(2).getTime(), rs.getString(3)),
                studentSystemId, routineId);
        return rows.stream().findFirst();
    }
}
