-- ============================================================================
-- Sub-subjects in an exam paper (user, 2026-11-12)
-- ============================================================================
-- exam_session.sub_subject_ids   comma-separated ids of the parent subject's sub-subjects
--                                sat in this paper (e.g. Drawing + Singing on the same day).
--                                NULL = the whole subject. Printed on the admit card as
--                                "Parent (Drawing, Singing)".
--
-- Idempotent — safe to re-run.
-- ============================================================================

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_session' AND COLUMN_NAME = 'sub_subject_ids');
SET @sql := IF(@col = 0, 'ALTER TABLE exam_session ADD COLUMN sub_subject_ids VARCHAR(255) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
