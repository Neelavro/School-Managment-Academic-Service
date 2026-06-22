-- ============================================================================
-- academic_service — Marking section
-- ============================================================================
-- Feature-specific tables for the "Marking" sidebar items:
--   Exam Components, Marking Structures, Mark Entry, Merge Subjects
--
-- (Merge Subjects is handled by class_subject_group.merge_group_id in
--  academic_structure.sql — no separate table for it.)
--
-- Foundation tables used:
--   subject, student_group                (bootstrap.sql)
--   class                                  (academic_structure.sql)
--   exam_type, exam_routine                (exams.sql)
--   enrollment                             (students.sql)
--
-- No circular FKs. No junction tables.
--
-- All data columns are nullable. App layer enforces "required" rules.
-- Idempotent — uses CREATE TABLE IF NOT EXISTS.
--
-- Run order:
--   bootstrap.sql → academic_structure.sql → exams.sql → students.sql → marking.sql
-- ============================================================================

-- ── 1. exam_component  (independent — "Written", "MCQ", "Viva", etc.) ─────
CREATE TABLE IF NOT EXISTS exam_component (
  id            INT          NOT NULL AUTO_INCREMENT,
  name          VARCHAR(255)     NULL,
  order_index   INT              NULL,
  is_active     BIT(1)           NULL DEFAULT b'1',
  deleted_at    DATETIME         NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_exam_component_name (name)
);

-- ── 2. marking_structure  (all FKs cross-section) ─────────────────────────
-- Total marks for a class+subject in a particular exam type, optionally
-- scoped to a student group. UNIQUE on the 4-column tuple.
CREATE TABLE IF NOT EXISTS marking_structure (
  id                INT          NOT NULL AUTO_INCREMENT,
  exam_type_id      INT              NULL,
  class_id          INT              NULL,
  subject_id        INT              NULL,
  group_id          INT              NULL,
  total_marks       INT              NULL,
  pass_marks        INT              NULL,
  created_at        DATETIME         NULL,
  last_modified_at  DATETIME         NULL,
  is_active         BIT(1)           NULL DEFAULT b'1',
  deleted_at        DATETIME         NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_ms_examtype_class_subject_group (exam_type_id, class_id, subject_id, group_id),
  KEY idx_ms_exam_type (exam_type_id),
  KEY idx_ms_class (class_id),
  KEY idx_ms_subject (subject_id),
  KEY idx_ms_group (group_id),
  CONSTRAINT fk_ms_exam_type FOREIGN KEY (exam_type_id)
      REFERENCES exam_type(id)     ON DELETE SET NULL,
  CONSTRAINT fk_ms_class     FOREIGN KEY (class_id)
      REFERENCES class(id)         ON DELETE SET NULL,
  CONSTRAINT fk_ms_subject   FOREIGN KEY (subject_id)
      REFERENCES subject(id)       ON DELETE SET NULL,
  CONSTRAINT fk_ms_group     FOREIGN KEY (group_id)
      REFERENCES student_group(id) ON DELETE SET NULL
);

-- ── 3. marking_structure_component  (max marks per component of a structure) ─
-- e.g. for a "Math, Class 10, Final" structure: Written=70, MCQ=20, Viva=10
CREATE TABLE IF NOT EXISTS marking_structure_component (
  id                      INT          NOT NULL AUTO_INCREMENT,
  marking_structure_id    INT              NULL,
  exam_component_id       INT              NULL,
  max_marks               INT              NULL,
  pass_marks              INT              NULL,
  created_at              DATETIME         NULL,
  last_modified_at        DATETIME         NULL,
  is_active               BIT(1)           NULL DEFAULT b'1',
  deleted_at              DATETIME         NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_msc_structure_component (marking_structure_id, exam_component_id),
  KEY idx_msc_structure (marking_structure_id),
  KEY idx_msc_component (exam_component_id),
  CONSTRAINT fk_msc_structure FOREIGN KEY (marking_structure_id)
      REFERENCES marking_structure(id) ON DELETE CASCADE,
  CONSTRAINT fk_msc_component FOREIGN KEY (exam_component_id)
      REFERENCES exam_component(id)    ON DELETE RESTRICT
);

-- ── 4. student_mark  (the actual marks entered per student per component) ──
-- enrollment_id, routine_id, subject_id are plain columns in Java (not @ManyToOne)
-- but are conceptually FKs — DB-level FKs added here for integrity.
-- UNIQUE on (enrollment_id, routine_id, subject_id, exam_component_id):
-- one mark per student per component within a routine.
CREATE TABLE IF NOT EXISTS student_mark (
  id                  BIGINT          NOT NULL AUTO_INCREMENT,
  enrollment_id       BIGINT              NULL,
  routine_id          INT                 NULL,
  subject_id          INT                 NULL,
  exam_component_id   INT                 NULL,
  marks_obtained      DECIMAL(10,2)       NULL,
  status              VARCHAR(20)         NULL,   -- "PRESENT" | "ABSENT" | "EXPELLED" | NULL
  created_at          DATETIME            NULL,
  last_modified_at    DATETIME            NULL,
  deleted_at          DATETIME            NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_sm (enrollment_id, routine_id, subject_id, exam_component_id),
  KEY idx_sm_routine_subject (routine_id, subject_id),
  KEY idx_sm_enrollment (enrollment_id),
  KEY idx_sm_component (exam_component_id),
  CONSTRAINT fk_sm_enrollment FOREIGN KEY (enrollment_id)
      REFERENCES enrollment(id)     ON DELETE CASCADE,
  CONSTRAINT fk_sm_routine    FOREIGN KEY (routine_id)
      REFERENCES exam_routine(id)   ON DELETE CASCADE,
  CONSTRAINT fk_sm_subject    FOREIGN KEY (subject_id)
      REFERENCES subject(id)        ON DELETE SET NULL,
  CONSTRAINT fk_sm_component  FOREIGN KEY (exam_component_id)
      REFERENCES exam_component(id) ON DELETE RESTRICT
);

-- ============================================================================
-- Verification block — what this file did
-- ============================================================================
-- Tables created                : 4   (exam_component, marking_structure,
--                                       marking_structure_component, student_mark)
-- Foreign keys added            : 10  (4 marking_structure + 2 marking_structure_component
--                                       + 4 student_mark)
-- Junction tables               : 0
-- Circular FKs                  : 0
-- ALTER TABLE statements        : 0
-- Unique constraints            : 4   (exam_component.name; marking_structure 4-col;
--                                       marking_structure_component 2-col;
--                                       student_mark 4-col)
-- DB FKs added that weren't @ManyToOne in Java : 3
--   (student_mark.enrollment_id, routine_id, subject_id — plain columns in entity)
-- Skipped @Transient fields     : 0
-- Soft-delete columns           : 4   (every table has deleted_at)
-- ============================================================================
