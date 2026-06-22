-- ============================================================================
-- academic_service — Results section
-- ============================================================================
-- The "Results" sidebar section has 6 items:
--   Session Results, Routine Results, Annual Results, Merit List,
--   Statistics, Publish Results
--
-- Of these, only "Publish Results" is backed by a dedicated table
-- (result_publication). The other five are COMPUTED VIEWS over student_mark,
-- exam_routine, marking_structure, etc. — no tables of their own.
--
-- Foundation used:
--   exam_routine                           (exams.sql)
--
-- No circular FKs. No junction tables.
--
-- All data columns are nullable. App layer enforces "required" rules.
-- Idempotent — uses CREATE TABLE IF NOT EXISTS.
--
-- Run order:
--   bootstrap.sql → academic_structure.sql → exams.sql → results.sql
-- ============================================================================

-- ── result_publication  (one row per routine — has it been published?) ────
-- @OneToOne on routine_id → UNIQUE constraint (only one publication per routine).
CREATE TABLE IF NOT EXISTS result_publication (
  id            INT          NOT NULL AUTO_INCREMENT,
  routine_id    INT              NULL,
  published     BIT(1)           NULL DEFAULT b'0',
  published_at  DATETIME         NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_result_publication_routine (routine_id),
  CONSTRAINT fk_result_pub_routine FOREIGN KEY (routine_id)
      REFERENCES exam_routine(id) ON DELETE CASCADE
);

-- ============================================================================
-- Verification block — what this file did
-- ============================================================================
-- Tables created                : 1   (result_publication)
-- Foreign keys added            : 1   (routine_id → exam_routine, cross-section)
-- Junction tables               : 0
-- Circular FKs                  : 0
-- ALTER TABLE statements        : 0
-- Unique constraints            : 1   (routine_id — from @OneToOne semantics)
-- Skipped @Transient fields     : 0
-- Soft-delete columns           : 0
--
-- Sidebar items WITHOUT a backing table (computed views in app code):
--   Session Results, Routine Results, Annual Results, Merit List, Statistics
-- ============================================================================
