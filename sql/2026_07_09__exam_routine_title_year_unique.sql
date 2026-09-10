-- ============================================================================
-- Migration: unique (title, academic_year_id) on exam_routine
-- ============================================================================
-- The old service-layer guard blocked multiple routines per (exam_type,
-- academic_year), which prevented the "one exam_type = TERM EXAM, N routines
-- per year (1st Term / 2nd Term / 3rd Term) sharing one marking structure"
-- workflow. That guard is now removed; uniqueness is enforced on the
-- combination of routine title + academic year instead, so accidental
-- duplicates still can't sneak in — the admin must pick a distinct title.
--
-- Fails if you already have two active routines with the same title in the
-- same year. Check first:
--
--   SELECT title, academic_year_id, COUNT(*) AS n
--   FROM   exam_routine
--   WHERE  is_active = 1
--   GROUP  BY title, academic_year_id
--   HAVING n > 1;
--
-- If any rows come back, rename or deactivate one before running this.
-- ============================================================================

ALTER TABLE exam_routine
  ADD CONSTRAINT uq_exam_routine_title_year UNIQUE (title, academic_year_id);
