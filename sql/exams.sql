-- ============================================================================
-- academic_service — Exams section
-- ============================================================================
-- Feature-specific tables for the "Exams" sidebar items:
--   Exam Types, Exam Routines
-- (Plus the supporting tables that the routine UI uses but aren't sidebar
-- items themselves: exam_session, exam_class_room_assignment.)
--
-- Foundation tables used:
--   academic_year, subject, student_group, room   (bootstrap.sql)
--   class                                         (academic_structure.sql)
--
-- No circular FKs. No junction tables.
--
-- All data columns are nullable. App layer enforces "required" rules.
-- Idempotent — uses CREATE TABLE IF NOT EXISTS.
--
-- Run order:
--   bootstrap.sql  →  academic_structure.sql  →  exams.sql
-- ============================================================================

-- ── 1. exam_type  (independent — "1st Term", "Mid Term", "Final") ─────────
CREATE TABLE IF NOT EXISTS exam_type (
  id            INT          NOT NULL AUTO_INCREMENT,
  name          VARCHAR(255)     NULL,
  order_index   INT              NULL,
  is_active     BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id),
  UNIQUE KEY uq_exam_type_name (name)
);

-- ── 2. exam_routine  (FK → exam_type same-section, academic_year bootstrap) ─
-- Multiple routines per exam_type in one year are allowed — e.g. one
-- exam_type "TERM EXAM" backing three routines "1st Term / 2nd Term /
-- 3rd Term" sharing one marking structure per (class, subject). Uniqueness
-- lives on (title, academic_year_id) so accidental duplicates are still
-- rejected — the admin must pick a distinct title within a year.
CREATE TABLE IF NOT EXISTS exam_routine (
  id                          INT          NOT NULL AUTO_INCREMENT,
  title                       VARCHAR(255)     NULL,
  exam_type_id                INT              NULL,
  academic_year_id            INT              NULL,
  status                      VARCHAR(20)      NULL DEFAULT 'DRAFT',  -- DRAFT | PUBLISHED
  published_at                DATETIME         NULL,
  created_at                  DATETIME         NULL,
  last_modified_at            DATETIME         NULL,
  is_active                   BIT(1)           NULL DEFAULT b'1',
  routine_start_date          DATE             NULL,
  routine_end_date            DATE             NULL,
  consider_for_annual_result  BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id),
  UNIQUE KEY uq_exam_routine_title_year (title, academic_year_id),
  KEY idx_routine_exam_type (exam_type_id),
  KEY idx_routine_academic_year (academic_year_id),
  KEY idx_routine_status (status),
  CONSTRAINT fk_exam_routine_type FOREIGN KEY (exam_type_id)
      REFERENCES exam_type(id)     ON DELETE RESTRICT,
  CONSTRAINT fk_exam_routine_year FOREIGN KEY (academic_year_id)
      REFERENCES academic_year(id) ON DELETE SET NULL
);

-- ── 3. exam_session  (FK → exam_routine, class, subject, student_group) ───
CREATE TABLE IF NOT EXISTS exam_session (
  id                    INT          NOT NULL AUTO_INCREMENT,
  exam_routine_id       INT              NULL,
  class_id              INT              NULL,
  subject_id            INT              NULL,
  date                  DATE             NULL,
  group_id              INT              NULL,
  start_time            TIME             NULL,
  end_time              TIME             NULL,
  show_on_admit_card    BIT(1)           NULL DEFAULT b'1',
  last_modified_at      DATETIME         NULL,
  is_active             BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id),
  KEY idx_es_routine_active (exam_routine_id, is_active),
  KEY idx_es_routine_class_active (exam_routine_id, class_id, is_active),
  KEY idx_es_subject (subject_id),
  KEY idx_es_group (group_id),
  CONSTRAINT fk_session_routine FOREIGN KEY (exam_routine_id)
      REFERENCES exam_routine(id)  ON DELETE CASCADE,
  CONSTRAINT fk_session_class   FOREIGN KEY (class_id)
      REFERENCES class(id)         ON DELETE SET NULL,
  CONSTRAINT fk_session_subject FOREIGN KEY (subject_id)
      REFERENCES subject(id)       ON DELETE SET NULL,
  CONSTRAINT fk_session_group   FOREIGN KEY (group_id)
      REFERENCES student_group(id) ON DELETE SET NULL
);

-- ── 4. exam_class_room_assignment  (FK → exam_routine, class, room) ───────
-- UNIQUE on (exam_routine_id, class_id, room_id) — same routine+class can't be
-- assigned to the same room twice.
CREATE TABLE IF NOT EXISTS exam_class_room_assignment (
  id                INT          NOT NULL AUTO_INCREMENT,
  exam_routine_id   INT              NULL,
  class_id          INT              NULL,
  room_id           INT              NULL,
  start_roll        INT              NULL,
  end_roll          INT              NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_ecra_routine_class_room (exam_routine_id, class_id, room_id),
  KEY idx_ecra_routine (exam_routine_id),
  KEY idx_ecra_class (class_id),
  KEY idx_ecra_room (room_id),
  CONSTRAINT fk_ecra_routine FOREIGN KEY (exam_routine_id)
      REFERENCES exam_routine(id) ON DELETE CASCADE,
  CONSTRAINT fk_ecra_class   FOREIGN KEY (class_id)
      REFERENCES class(id)        ON DELETE SET NULL,
  CONSTRAINT fk_ecra_room    FOREIGN KEY (room_id)
      REFERENCES room(id)         ON DELETE SET NULL
);

-- ============================================================================
-- Verification block — what this file did
-- ============================================================================
-- Tables created                : 4   (exam_type, exam_routine, exam_session,
--                                       exam_class_room_assignment)
-- Foreign keys added            : 9   (1 same-section + 8 cross-section)
-- Junction tables               : 0
-- Circular FKs                  : 0
-- ALTER TABLE statements        : 0   (no cross-FKs needed deferred handling)
-- Unique constraints            : 2   (exam_type.name; ECRA composite)
-- Skipped @Transient fields     : 1   (exam_routine.result_published)
-- ============================================================================
