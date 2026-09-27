-- ============================================================================
-- Updating published results: check, review, apply
-- ============================================================================
-- Stored results are a snapshot: marks and marking structures stay editable.
-- To update a published class without taking it down, a CHECK job recomputes
-- every result and saves only the differences in result_update_change. The
-- admin reviews them, then applies them (only those rows of published_result
-- are written) or discards them.
--
-- Requires 2026_09_27__published_results.sql. Idempotent — safe to re-run.
-- ============================================================================

-- ── result_publish_job: job kind and a check's counts ─────────────────────
-- kind:   PUBLISH | CHECK
-- status: QUEUED → PROCESSING → DONE | FAILED; a CHECK that is DONE holds a
--         preview, which then becomes APPLIED or DISCARDED.
DROP PROCEDURE IF EXISTS add_result_job_columns;
DELIMITER //
CREATE PROCEDURE add_result_job_columns()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'result_publish_job' AND COLUMN_NAME = 'kind') THEN
    ALTER TABLE result_publish_job
      ADD COLUMN kind    VARCHAR(10) NOT NULL DEFAULT 'PUBLISH' AFTER class_id,
      ADD COLUMN changed INT NULL AFTER error,
      ADD COLUMN added   INT NULL AFTER changed,
      ADD COLUMN removed INT NULL AFTER added;
  END IF;
END //
DELIMITER ;
CALL add_result_job_columns();
DROP PROCEDURE add_result_job_columns;

-- ── result_update_change  (one row per student whose result would change) ──
-- change_type: CHANGED | ADDED (newly enrolled) | REMOVED (no longer enrolled)
-- diff_json:   what the admin reviews (old and new values)
-- result_json and the summary columns: the new stored row, written on apply
-- Rows are deleted when the check is applied or discarded.
CREATE TABLE IF NOT EXISTS result_update_change (
  id                 BIGINT        NOT NULL AUTO_INCREMENT,
  job_id             BIGINT        NOT NULL,
  enrollment_id      BIGINT        NOT NULL,
  student_system_id  VARCHAR(255)      NULL,
  student_name       VARCHAR(255)      NULL,
  class_roll         INT               NULL,
  change_type        VARCHAR(10)   NOT NULL,
  diff_json          MEDIUMTEXT    NOT NULL,
  total_marks        DECIMAL(10,2)     NULL,
  gpa                DOUBLE            NULL,
  passed             BIT(1)            NULL,
  result_json        MEDIUMTEXT        NULL,
  PRIMARY KEY (id),
  KEY idx_result_update_change_job (job_id, class_roll)
);
