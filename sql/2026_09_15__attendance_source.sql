-- Adds `source` to attendance so the Steller reconciler can distinguish
-- auto-derived rows from teacher/admin manual entries. Existing rows are
-- treated as MANUAL (they were all created by a human before this).
ALTER TABLE attendance
  ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'MANUAL' AFTER date;

ALTER TABLE attendance
  ADD KEY idx_att_source (source);
