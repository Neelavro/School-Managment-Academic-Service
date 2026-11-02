-- ============================================================================
-- Working days + student attendance on the progress report (user, 2026-11-02)
-- ============================================================================
-- exam_routine.working_days: how many class days the exam's term had (one number
-- per exam routine, every class). exam_attendance: how many of those days each
-- student attended, typed in by hand on Progress Report before downloading.
-- Absent = working days - present (computed, not stored).
--
-- Idempotent — safe to re-run. Adds an empty column and an empty table.
-- ============================================================================

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_routine' AND COLUMN_NAME = 'working_days');
SET @sql := IF(@col = 0, 'ALTER TABLE exam_routine ADD COLUMN working_days INT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS exam_attendance (
  id               BIGINT      NOT NULL AUTO_INCREMENT,
  exam_routine_id  INT         NOT NULL,
  enrollment_id    BIGINT      NOT NULL,
  present_days     INT         NOT NULL,
  updated_at       DATETIME(6)     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_exam_attendance (exam_routine_id, enrollment_id),
  CONSTRAINT fk_ea_routine    FOREIGN KEY (exam_routine_id) REFERENCES exam_routine(id) ON DELETE CASCADE,
  CONSTRAINT fk_ea_enrollment FOREIGN KEY (enrollment_id)   REFERENCES enrollment(id)   ON DELETE CASCADE
) ENGINE=InnoDB;
