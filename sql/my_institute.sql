-- ============================================================================
-- academic_service — Institute Setup section
-- ============================================================================
-- Feature-specific tables for the "Institute Setup" sidebar items:
--   Academic Calendar, Holidays, Exam Days, Ramadan
--
-- Foundation tables used by this section are now in bootstrap.sql:
--   system_settings, room, grading_policies, grades
-- So this file only contains tables that have an FK to them.
--
-- All data columns are nullable. App layer enforces "required" rules.
-- Idempotent — uses CREATE TABLE IF NOT EXISTS.
--
-- Run order: bootstrap.sql  →  my_institute.sql
-- ============================================================================

-- ── academic_calendar_entry  (FK → academic_year from bootstrap) ───────────
-- Single table backs four sidebar items — filtered views by `type`:
--   Academic Calendar → all entries
--   Holidays          → type = 'HOLIDAY'
--   Exam Days         → type = 'EXAM_DAY'
--   Ramadan           → type = 'RAMADAN'
CREATE TABLE IF NOT EXISTS academic_calendar_entry (
  id                  INT          NOT NULL AUTO_INCREMENT,
  academic_year_id    INT              NULL,
  type                VARCHAR(20)      NULL,               -- HOLIDAY | EXAM_DAY | RAMADAN
  name                VARCHAR(255)     NULL,
  start_date          DATE             NULL,
  end_date            DATE             NULL,
  created_at          DATETIME         NULL,
  is_active           BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id),
  KEY idx_calendar_year (academic_year_id),
  KEY idx_calendar_type (type),
  KEY idx_calendar_dates (start_date, end_date),
  CONSTRAINT fk_calendar_year FOREIGN KEY (academic_year_id)
      REFERENCES academic_year(id) ON DELETE RESTRICT
);
