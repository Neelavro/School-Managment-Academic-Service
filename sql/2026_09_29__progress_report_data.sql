-- ============================================================================
-- Student progress reports drawn by the portal
-- ============================================================================
-- published_result.report_json holds each student's progress-report data
-- (subjects, components, highest marks, positions, merged-subject grades and
-- the grading table), built at publish with the same code as the admin
-- progress-report PDF. A "check for changes" recomputes it for the whole class
-- into result_update_report; applying the check copies it over.
-- The app fills report_json for already-published classes on startup.
--
-- Requires 2026_09_28__result_update_check.sql. Idempotent — safe to re-run.
-- ============================================================================

DROP PROCEDURE IF EXISTS add_report_json_column;
DELIMITER //
CREATE PROCEDURE add_report_json_column()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'published_result' AND COLUMN_NAME = 'report_json') THEN
    ALTER TABLE published_result ADD COLUMN report_json MEDIUMTEXT NULL AFTER result_json;
  END IF;
END //
DELIMITER ;
CALL add_report_json_column();
DROP PROCEDURE add_report_json_column;

-- ── result_update_report  (a check's new report data, one row per student) ──
-- Deleted when the check is applied or discarded.
CREATE TABLE IF NOT EXISTS result_update_report (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  job_id         BIGINT       NOT NULL,
  enrollment_id  BIGINT       NOT NULL,
  report_json    MEDIUMTEXT   NOT NULL,
  PRIMARY KEY (id),
  KEY idx_result_update_report_job (job_id, enrollment_id)
);
