-- ============================================================================
-- Academic calendar: year start date, Exam Days linked to exam routines, school
-- weekends; class routine room optional (user, 2026-11-03)
-- ============================================================================
-- academic_year.start_date      first day of the academic year (a June-to-June school
--                               sets 1 June); the year ends a year later minus a day.
--                               Existing years named like "2026" get 1 January.
-- academic_calendar_entry.exam_routine_id
--                               the exam an Exam Days entry belongs to (its name comes
--                               from the routine). Existing EXAM_DAY entries are linked
--                               to the routine of the same year with the same title.
-- system_settings.weekend_days  the school's weekly off days, e.g. "FRIDAY" or
--                               "FRIDAY,SATURDAY" (NULL = Friday).
-- class_routine.room_id         now optional.
--
-- Idempotent — safe to re-run.
-- ============================================================================

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'academic_year' AND COLUMN_NAME = 'start_date');
SET @sql := IF(@col = 0, 'ALTER TABLE academic_year ADD COLUMN start_date DATE NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE academic_year
   SET start_date = STR_TO_DATE(CONCAT(year_name, '-01-01'), '%Y-%m-%d')
 WHERE start_date IS NULL AND year_name REGEXP '^[0-9]{4}$';

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'academic_calendar_entry' AND COLUMN_NAME = 'exam_routine_id');
SET @sql := IF(@col = 0, 'ALTER TABLE academic_calendar_entry ADD COLUMN exam_routine_id INT NULL, ADD CONSTRAINT fk_ace_exam_routine FOREIGN KEY (exam_routine_id) REFERENCES exam_routine(id) ON DELETE SET NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE academic_calendar_entry ace
  JOIN exam_routine er
    ON er.academic_year_id = ace.academic_year_id
   AND UPPER(TRIM(er.title)) = UPPER(TRIM(ace.name))
   AND er.is_active = 1
   SET ace.exam_routine_id = er.id
 WHERE ace.type = 'EXAM_DAY' AND ace.exam_routine_id IS NULL;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_settings' AND COLUMN_NAME = 'weekend_days');
SET @sql := IF(@col = 0, 'ALTER TABLE system_settings ADD COLUMN weekend_days VARCHAR(64) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE class_routine MODIFY COLUMN room_id INT NULL;
