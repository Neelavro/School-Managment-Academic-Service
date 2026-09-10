-- ============================================================================
-- Migration: add order_index to subject (global subject ordering)
-- ============================================================================
-- Progress reports, marksheets, and result responses previously rendered
-- subjects in whatever order the exam_session query returned them. This adds
-- a global order_index on subject so admins can set a canonical order that
-- applies across every class (Bangla → English → Math → Science → …).
--
-- Papers within a merge group are still ordered by class_subject_group
-- .merge_order_index; the new order_index only affects order between subjects
-- (or between merge groups, anchored on the min order_index of their members).
--
-- Existing rows default to 0 — until admins set values, the visible order
-- matches the previous behavior (grouped by whatever the query returns).
-- ============================================================================

ALTER TABLE subject
  ADD COLUMN order_index INT NULL DEFAULT 0;
