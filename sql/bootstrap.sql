-- ============================================================================
-- academic_service — bootstrap (foundation tables)
-- ============================================================================
-- The pre-req of pre-reqs. Every other section file references tables defined
-- here. Run this BEFORE any section file.
--
-- All data columns are nullable. App layer enforces "required" rules; schema
-- stays lenient so admins can save partial drafts.
--
-- Idempotent — uses CREATE TABLE IF NOT EXISTS.
--
-- Run order:
--   bootstrap.sql  →  my_institute.sql / academic_structure.sql / students.sql
--                  →  exams.sql / marking.sql / results.sql / ...
--                  →  accounts_schema.sql
-- ============================================================================

-- ── 1. system_settings  (singleton row holds branding) ─────────────────────
CREATE TABLE IF NOT EXISTS system_settings (
  id                INT          NOT NULL AUTO_INCREMENT,
  institution_name  VARCHAR(255)     NULL,
  address           TEXT             NULL,
  logo_url          VARCHAR(255)     NULL,
  heading           VARCHAR(255)     NULL,
  signature_url     VARCHAR(255)     NULL,
  PRIMARY KEY (id)
);

-- ── 2. academic_year  (referenced by calendar entries, enrollments, etc.) ──
CREATE TABLE IF NOT EXISTS academic_year (
  id          INT          NOT NULL AUTO_INCREMENT,
  year_name   VARCHAR(255)     NULL,
  is_active   BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id)
);

-- ── 3. shift  (referenced by class, enrollment) ────────────────────────────
CREATE TABLE IF NOT EXISTS shift (
  id          INT          NOT NULL AUTO_INCREMENT,
  name        VARCHAR(255)     NULL,
  is_active   BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id)
);

-- ── 4. gender  (referenced by student) ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS gender (
  id          INT          NOT NULL AUTO_INCREMENT,
  gender      VARCHAR(255)     NULL,
  is_active   BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id)
);

-- ── 5. student_status  (referenced by student) ─────────────────────────────
CREATE TABLE IF NOT EXISTS student_status (
  id            INT          NOT NULL AUTO_INCREMENT,
  status_name   VARCHAR(50)      NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_student_status_name (status_name)
);

-- ── 6. gender_section  (e.g. Boys, Girls, Co-ed) ───────────────────────────
CREATE TABLE IF NOT EXISTS gender_section (
  id          INT          NOT NULL AUTO_INCREMENT,
  gender_name VARCHAR(255)     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_gender_section_name (gender_name)
);

-- ── 7. student_group  (Science/Commerce/Arts/etc.) ─────────────────────────
CREATE TABLE IF NOT EXISTS student_group (
  id          INT          NOT NULL AUTO_INCREMENT,
  group_name  VARCHAR(255)     NULL,
  is_active   BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id)
);

-- ── 8. subject  (referenced by routine, csg, overrides, etc.) ──────────────
CREATE TABLE IF NOT EXISTS subject (
  id          INT          NOT NULL AUTO_INCREMENT,
  name        VARCHAR(255)     NULL,
  code        VARCHAR(255)     NULL,
  order_index INT              NULL DEFAULT 0,
  is_active   BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id)
);

-- ── 9. room  (referenced by class_routine, exams) ──────────────────────────
CREATE TABLE IF NOT EXISTS room (
  id          INT          NOT NULL AUTO_INCREMENT,
  name        VARCHAR(255)     NULL,
  capacity    INT              NULL,
  is_active   BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id),
  UNIQUE KEY uq_room_name (name)
);

-- ── 10. grading_policies  (referenced by class) ────────────────────────────
CREATE TABLE IF NOT EXISTS grading_policies (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(255)     NULL,
  is_active   BIT(1)           NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_grading_policy_name (name)
);

-- ── 11. grades  (depends on grading_policies — grade bands) ────────────────
CREATE TABLE IF NOT EXISTS grades (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  grading_policy_id   BIGINT           NULL,
  name                VARCHAR(255)     NULL,
  gpa_value           DOUBLE           NULL,
  min_mark            DOUBLE           NULL,
  max_mark            DOUBLE           NULL,
  comment             VARCHAR(255)     NULL,
  PRIMARY KEY (id),
  KEY idx_grades_policy (grading_policy_id),
  CONSTRAINT fk_grades_policy FOREIGN KEY (grading_policy_id)
      REFERENCES grading_policies(id) ON DELETE CASCADE
);
