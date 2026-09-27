package com.example.academic_service.service;

import com.example.academic_service.dto.result_dtos.StudentRoutineResultResponse;
import com.example.academic_service.entity.Enrollment;
import com.example.academic_service.entity.ExamRoutine;
import com.example.academic_service.repository.EnrollmentRepository;
import com.example.academic_service.repository.ExamRoutineRepository;
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
 * Publishing a class queues a job. One worker thread processes jobs one class at a time:
 * it computes every active student's result, then in one transaction replaces the class's
 * published_result rows and marks the class published, so students never see half a class.
 * Unpublishing deletes the rows. While a class is published (or being published) its marks
 * and marking structures are locked, so a stored result can't silently go stale.
 */
@Service
@RequiredArgsConstructor
public class ResultPublishService {

    private static final Logger log = LoggerFactory.getLogger(ResultPublishService.class);

    public static final String QUEUED = "QUEUED";
    public static final String PROCESSING = "PROCESSING";
    public static final String DONE = "DONE";
    public static final String FAILED = "FAILED";

    private static final int CHUNK = 25;

    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager transactionManager;
    private final ResultService resultService;
    private final EnrollmentRepository enrollmentRepository;
    private final ExamRoutineRepository examRoutineRepository;
    private final JsonMapper objectMapper; // Spring's own HTTP mapper, so stored JSON matches what the API used to send

    // One thread: classes are published one at a time so a "publish all" doesn't swamp the CPU.
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

    public boolean isPublishing(Integer routineId, Integer classId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM result_publish_job WHERE routine_id = ? AND class_id = ? AND status IN (?, ?)",
                Integer.class, routineId, classId, QUEUED, PROCESSING);
        return n != null && n > 0;
    }

    /** Queues a publish job for one class and returns its id. */
    public long enqueue(Integer routineId, Integer classId) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO result_publish_job (routine_id, class_id, status, done, created_at) VALUES (?, ?, ?, 0, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, routineId);
            ps.setInt(2, classId);
            ps.setString(3, QUEUED);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
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
                jdbc.update("INSERT INTO result_publish_job (routine_id, class_id, status, done, created_at) VALUES (?, ?, ?, 0, ?)",
                        m.get("routine_id"), m.get("class_id"), QUEUED, Timestamp.valueOf(LocalDateTime.now()));
            }
            List<Long> queued = jdbc.queryForList(
                    "SELECT id FROM result_publish_job WHERE status = ? ORDER BY id", Long.class, QUEUED);
            queued.forEach(id -> worker.submit(() -> process(id)));
            if (!queued.isEmpty())
                log.info("Result publish: {} job(s) queued on startup ({} backfill)", queued.size(), missing.size());
        } catch (Exception e) {
            // Most likely the migration hasn't been run yet; the portal would fail loudly anyway.
            log.error("Result publish: startup resume failed — is sql/2026_09_27__published_results.sql applied? {}",
                    e.getMessage());
        }
    }

    // ── Worker ───────────────────────────────────────────────────────────────

    private void process(Long jobId) {
        // Claim the job; skip it if another submit already took it.
        if (jdbc.update("UPDATE result_publish_job SET status = ?, started_at = ? WHERE id = ? AND status = ?",
                PROCESSING, Timestamp.valueOf(LocalDateTime.now()), jobId, QUEUED) == 0) return;

        Map<String, Object> job = jdbc.queryForMap("SELECT routine_id, class_id FROM result_publish_job WHERE id = ?", jobId);
        Integer routineId = ((Number) job.get("routine_id")).intValue();
        Integer classId = ((Number) job.get("class_id")).intValue();
        long started = System.currentTimeMillis();
        try {
            ExamRoutine routine = examRoutineRepository.findById(routineId)
                    .orElseThrow(() -> new IllegalStateException("Exam routine not found: " + routineId));
            if (routine.getAcademicYear() == null)
                throw new IllegalStateException("Exam routine has no academic year");
            Integer academicYearId = routine.getAcademicYear().getId();

            List<Enrollment> enrollments = enrollmentRepository
                    .findByStudentClass_IdAndAcademicYear_IdAndIsActiveTrue(classId, academicYearId);
            jdbc.update("UPDATE result_publish_job SET total = ? WHERE id = ?", enrollments.size(), jobId);

            List<Object[]> rows = new ArrayList<>();
            List<String> skipped = new ArrayList<>();
            Timestamp computedAt = Timestamp.valueOf(LocalDateTime.now());
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
                            rows.add(new Object[]{routineId, classId, e.getId(), e.getStudentSystemId(),
                                    r.getTotalMarks(), r.getOverallGpa(), r.isPassed(),
                                    objectMapper.writeValueAsString(r), computedAt});
                        } catch (Exception ex) {
                            skipped.add(e.getStudentSystemId() + ": " + ex.getMessage());
                        }
                    }
                });
                jdbc.update("UPDATE result_publish_job SET done = ? WHERE id = ?", rows.size() + skipped.size(), jobId);
            }

            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                jdbc.update("DELETE FROM published_result WHERE routine_id = ? AND class_id = ?", routineId, classId);
                jdbc.batchUpdate("""
                        INSERT INTO published_result (routine_id, class_id, enrollment_id, student_system_id,
                                                      total_marks, gpa, passed, result_json, computed_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, rows);
                int updated = jdbc.update("""
                        UPDATE result_publication SET published = 1, published_at = COALESCE(published_at, ?)
                        WHERE routine_id = ? AND class_id = ?
                        """, Timestamp.valueOf(LocalDateTime.now()), routineId, classId);
                if (updated == 0)
                    jdbc.update("INSERT INTO result_publication (routine_id, class_id, published, published_at) VALUES (?, ?, 1, ?)",
                            routineId, classId, Timestamp.valueOf(LocalDateTime.now()));
            });

            String note = skipped.isEmpty() ? null : truncate(skipped.size() + " student(s) skipped. First: " + skipped.get(0));
            jdbc.update("UPDATE result_publish_job SET status = ?, done = ?, error = ?, finished_at = ? WHERE id = ?",
                    DONE, rows.size() + skipped.size(), note, Timestamp.valueOf(LocalDateTime.now()), jobId);
            log.info("Result publish: routine={} class={} stored {} result(s), skipped {} in {} ms",
                    routineId, classId, rows.size(), skipped.size(), System.currentTimeMillis() - started);
        } catch (Exception e) {
            if (stopping) {
                // Interrupted by shutdown: leave it PROCESSING so the next startup runs it again.
                log.warn("Result publish interrupted by shutdown: job={} routine={} class={}", jobId, routineId, classId);
                return;
            }
            log.error("Result publish failed: job={} routine={} class={}", jobId, routineId, classId, e);
            jdbc.update("UPDATE result_publish_job SET status = ?, error = ?, finished_at = ? WHERE id = ?",
                    FAILED, truncate(e.getMessage() != null ? e.getMessage() : e.toString()),
                    Timestamp.valueOf(LocalDateTime.now()), jobId);
        }
    }

    private static String truncate(String s) {
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }

    // ── Unpublish ────────────────────────────────────────────────────────────

    /** Hides the class's results and deletes the stored rows, in one transaction. */
    public void unpublish(Integer routineId, Integer classId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("UPDATE result_publication SET published = 0, published_at = NULL WHERE routine_id = ? AND class_id = ?",
                    routineId, classId);
            jdbc.update("DELETE FROM published_result WHERE routine_id = ? AND class_id = ?", routineId, classId);
        });
    }

    // ── Status (admin publish screen) ────────────────────────────────────────

    /** Latest job per class for a routine: classId → {status, done, total, error}. */
    public Map<Integer, Map<String, Object>> latestJobs(Integer routineId) {
        Map<Integer, Map<String, Object>> byClass = new HashMap<>();
        for (Map<String, Object> j : jdbc.queryForList(
                "SELECT class_id, status, done, total, error FROM result_publish_job WHERE routine_id = ? ORDER BY id DESC",
                routineId)) {
            byClass.putIfAbsent(((Number) j.get("class_id")).intValue(), j);
        }
        return byClass;
    }

    // ── Locks ────────────────────────────────────────────────────────────────

    /** Marks for a routine × class can't change while its results are published or being published. */
    public void assertMarksEditable(Integer routineId, Integer classId) {
        Integer published = jdbc.queryForObject(
                "SELECT COUNT(*) FROM result_publication WHERE routine_id = ? AND class_id = ? AND published = 1",
                Integer.class, routineId, classId);
        if ((published != null && published > 0) || isPublishing(routineId, classId))
            throw new RuntimeException("Results for this class are published. Unpublish them before changing marks.");
    }

    /** A marking structure applies to every routine of its exam type, so check them all. */
    public void assertStructureEditable(Integer examTypeId, Integer classId) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM exam_routine r
                WHERE r.exam_type_id = ?
                  AND (EXISTS (SELECT 1 FROM result_publication rp
                               WHERE rp.routine_id = r.id AND rp.class_id = ? AND rp.published = 1)
                    OR EXISTS (SELECT 1 FROM result_publish_job j
                               WHERE j.routine_id = r.id AND j.class_id = ? AND j.status IN (?, ?)))
                """, Integer.class, examTypeId, classId, classId, QUEUED, PROCESSING);
        if (n != null && n > 0)
            throw new RuntimeException("Results using this marking structure are published for this class. "
                    + "Unpublish them before changing it.");
    }

    // ── Student portal ───────────────────────────────────────────────────────

    public record StoredResult(long id, String json) {}

    /**
     * The student's stored result for a routine, or empty if it isn't published.
     * A row only exists while the class is published, so this is also the publication check.
     */
    public Optional<StoredResult> findForStudent(String studentSystemId, Integer routineId) {
        List<StoredResult> rows = jdbc.query(
                "SELECT id, result_json FROM published_result WHERE student_system_id = ? AND routine_id = ? ORDER BY id DESC LIMIT 1",
                (rs, i) -> new StoredResult(rs.getLong(1), rs.getString(2)), studentSystemId, routineId);
        return rows.stream().findFirst();
    }
}
