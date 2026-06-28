-- ============================================================================
-- academic_service — Academic Structure section
-- ============================================================================
-- Feature-specific tables for the "Academic Structure" sidebar items:
--   Classes, Sections, Class Routine
--
-- Foundation tables used by this section are in bootstrap.sql:
--   academic_year, shift, gender_section, student_group, subject,
--   room, grading_policies
-- The sidebar items "Academic Years / Shifts / Gender Sections / Groups /
-- Subjects" map to those bootstrap tables — they have no feature-specific
-- tables of their own.
--
-- All data columns are nullable. App layer enforces "required" rules.
-- Idempotent — uses CREATE TABLE IF NOT EXISTS.
--
-- Run order: bootstrap.sql  →  academic_structure.sql
-- ============================================================================

-- ── 1. class  (FK → shift, grading_policies — both from bootstrap) ─────────
CREATE TABLE IF NOT EXISTS class (
  id                  INT          NOT NULL AUTO_INCREMENT,
  name                VARCHAR(255)     NULL,
  shift_id            INT              NULL,
  grading_policy_id   BIGINT           NULL,
  use_gpa_for_result  BIT(1)           NULL DEFAULT b'0',
  order_index         INT              NULL,
  is_active           BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id),
  KEY idx_class_shift (shift_id),
  KEY idx_class_grading_policy (grading_policy_id),
  CONSTRAINT fk_class_shift FOREIGN KEY (shift_id)
      REFERENCES shift(id) ON DELETE SET NULL,
  CONSTRAINT fk_class_grading_policy FOREIGN KEY (grading_policy_id)
      REFERENCES grading_policies(id) ON DELETE SET NULL
);

-- ── 2. class_student_group  (junction: class ↔ student_group, M2M) ─────────
CREATE TABLE IF NOT EXISTS class_student_group (
  class_id          INT          NOT NULL,
  student_group_id  INT          NOT NULL,
  PRIMARY KEY (class_id, student_group_id),
  KEY idx_csg_student_group (student_group_id),
  CONSTRAINT fk_csg_class
      FOREIGN KEY (class_id)         REFERENCES class(id)         ON DELETE CASCADE,
  CONSTRAINT fk_csg_student_group
      FOREIGN KEY (student_group_id) REFERENCES student_group(id) ON DELETE CASCADE
);

-- ── 3. section  (FK → class, gender_section) ───────────────────────────────
CREATE TABLE IF NOT EXISTS section (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  section_name  VARCHAR(255)     NULL,
  class_id      INT              NULL,
  gender_id     INT              NULL,
  is_active     BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id),
  KEY idx_section_class (class_id),
  KEY idx_section_gender (gender_id),
  CONSTRAINT fk_section_class FOREIGN KEY (class_id)
      REFERENCES class(id) ON DELETE RESTRICT,
  CONSTRAINT fk_section_gender FOREIGN KEY (gender_id)
      REFERENCES gender_section(id) ON DELETE SET NULL
);

-- ── 4. class_subject_group  (which subjects a class+group studies) ─────────
CREATE TABLE IF NOT EXISTS class_subject_group (
  id                    INT          NOT NULL AUTO_INCREMENT,
  class_id              INT              NULL,
  subject_id            INT              NULL,
  student_group_id      INT              NULL,
  is_active             BIT(1)           NULL DEFAULT b'1',
  is_fourth_subject     BIT(1)           NULL DEFAULT b'0',
  merge_group_id        INT              NULL,
  merge_order_index     INT              NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uq_csg_class_subject_group (class_id, subject_id, student_group_id),
  KEY idx_csubg_class (class_id),
  KEY idx_csubg_subject (subject_id),
  KEY idx_csubg_student_group (student_group_id),
  CONSTRAINT fk_csubg_class
      FOREIGN KEY (class_id)         REFERENCES class(id)         ON DELETE CASCADE,
  CONSTRAINT fk_csubg_subject
      FOREIGN KEY (subject_id)       REFERENCES subject(id)       ON DELETE RESTRICT,
  CONSTRAINT fk_csubg_student_group
      FOREIGN KEY (student_group_id) REFERENCES student_group(id) ON DELETE SET NULL
);

-- ── 5. class_routine  (weekly timetable per class+section+subject+room) ───
-- class_id, gender_section_id and room_id are NOT NULL — the JPA entity
-- declares @JoinColumn(nullable = false) on each, and the prod schema
-- matches. section_id, student_group_id and subject_id remain optional.
CREATE TABLE IF NOT EXISTS class_routine (
  id                    INT          NOT NULL AUTO_INCREMENT,
  class_id              INT          NOT NULL,
  gender_section_id     INT          NOT NULL,
  section_id            BIGINT           NULL,
  student_group_id      INT              NULL,
  subject_id            INT              NULL,
  room_id               INT          NOT NULL,
  day_of_week           VARCHAR(20)  NOT NULL,                    -- MONDAY | TUESDAY | ...
  start_time            TIME         NOT NULL,
  end_time              TIME         NOT NULL,
  routine_type          VARCHAR(20)  NOT NULL DEFAULT 'DEFAULT',  -- DEFAULT | RAMADAN
  is_active             BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id),
  KEY idx_routine_class (class_id),
  KEY idx_routine_gender_section (gender_section_id),
  KEY idx_routine_section (section_id),
  KEY idx_routine_student_group (student_group_id),
  KEY idx_routine_subject (subject_id),
  KEY idx_routine_room (room_id),
  KEY idx_routine_day (day_of_week),
  CONSTRAINT fk_routine_class
      FOREIGN KEY (class_id)          REFERENCES class(id)          ON DELETE CASCADE,
  CONSTRAINT fk_routine_gender_section
      FOREIGN KEY (gender_section_id) REFERENCES gender_section(id) ON DELETE CASCADE,
  CONSTRAINT fk_routine_section
      FOREIGN KEY (section_id)        REFERENCES section(id)        ON DELETE SET NULL,
  CONSTRAINT fk_routine_student_group
      FOREIGN KEY (student_group_id)  REFERENCES student_group(id)  ON DELETE SET NULL,
  CONSTRAINT fk_routine_subject
      FOREIGN KEY (subject_id)        REFERENCES subject(id)        ON DELETE SET NULL,
  CONSTRAINT fk_routine_room
      FOREIGN KEY (room_id)           REFERENCES room(id)           ON DELETE CASCADE
);
