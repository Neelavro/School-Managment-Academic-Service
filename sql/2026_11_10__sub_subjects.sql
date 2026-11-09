-- ============================================================================
-- Sub-subjects (user, 2026-11-10)
-- ============================================================================
-- subject.parent_subject_id   the subject this one belongs to. Bangladesh co-curricular
--                             subjects are made of parts (Drawing, Singing, …) that add up
--                             to the parent's marks. One level only: a sub-subject can't
--                             have its own sub-subjects. NULL = an ordinary subject.
--
-- Idempotent — safe to re-run.
-- ============================================================================

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'subject' AND COLUMN_NAME = 'parent_subject_id');
SET @sql := IF(@col = 0, 'ALTER TABLE subject ADD COLUMN parent_subject_id INT NULL, ADD CONSTRAINT fk_subject_parent FOREIGN KEY (parent_subject_id) REFERENCES subject(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
