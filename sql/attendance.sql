-- ============================================================================
-- academic_service — Attendance section
-- ============================================================================
-- Two tables that don't have a sidebar section in the production-full-system
-- branch (Attendance was removed from the admin sidebar) but still exist as
-- @Entity classes that Hibernate loads on startup.
--
-- They drive the TEACHER PORTAL's attendance + class-teacher features.
-- Tables MUST exist for the app to start cleanly.
--
-- Foundation tables used:
--   academic_year       (bootstrap.sql)
--   section             (academic_structure.sql)
--   enrollment          (students.sql)
--   staff               (hr_management.sql)
--
-- No circular FKs. No junction tables. No same-section dependencies.
--
-- All data columns are nullable. App layer enforces "required" rules.
-- Idempotent — uses CREATE TABLE IF NOT EXISTS.
--
-- Run order:
--   bootstrap.sql → academic_structure.sql → students.sql
--                → hr_management.sql → attendance.sql
-- ============================================================================

-- ── 1. attendance  (per-student per-day absence record) ───────────────────
-- "Absent" is the recorded state; presence is implied for days with no row.
-- UNIQUE on (enrollment_id, date) — only one record per student per day.
CREATE TABLE IF NOT EXISTS attendance (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  enrollment_id   BIGINT           NULL,
  date            DATE             NULL,
  created_at      DATETIME         NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_attendance (enrollment_id, date),
  KEY idx_att_date (date),
  CONSTRAINT fk_attendance_enrollment FOREIGN KEY (enrollment_id)
      REFERENCES enrollment(id) ON DELETE CASCADE
);

-- source: 'MANUAL' = teacher/admin marked. 'STELLER' = auto-derived from the
-- biometric device sync (student did not punch that day). Teacher save always
-- overwrites both sources for the (section, date) window.
ALTER TABLE attendance
    ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'MANUAL' AFTER date;

ALTER TABLE attendance
    ADD KEY idx_att_source (source);

-- ── 2. class_teacher  (which staff is the class-teacher of a register/year) ─
-- A register is a section, or — when a class has no sections for a gender
-- section — the class + gender section (section_id NULL). class_id and
-- gender_section_id are always filled. One class teacher per register per
-- year: UNIQUE (section_id, academic_year_id) for sections; the app checks
-- class + gender section registers (MySQL UNIQUE ignores NULL section_id).
CREATE TABLE IF NOT EXISTS class_teacher (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  staff_id           BIGINT           NULL,
  class_id           INT              NULL,
  gender_section_id  INT              NULL,
  section_id         BIGINT           NULL,
  academic_year_id   INT              NULL,
  created_at         DATETIME         NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_class_teacher (section_id, academic_year_id),
  KEY idx_ct_staff (staff_id),
  KEY idx_ct_class_gender (class_id, gender_section_id, academic_year_id),
  KEY idx_ct_section (section_id),
  KEY idx_ct_year (academic_year_id),
  CONSTRAINT fk_ct_staff   FOREIGN KEY (staff_id)
      REFERENCES staff(id)         ON DELETE CASCADE,
  CONSTRAINT fk_ct_class   FOREIGN KEY (class_id)
      REFERENCES class(id)         ON DELETE CASCADE,
  CONSTRAINT fk_ct_gender_section FOREIGN KEY (gender_section_id)
      REFERENCES gender_section(id) ON DELETE SET NULL,
  CONSTRAINT fk_ct_section FOREIGN KEY (section_id)
      REFERENCES section(id)       ON DELETE SET NULL,
  CONSTRAINT fk_ct_year    FOREIGN KEY (academic_year_id)
      REFERENCES academic_year(id) ON DELETE SET NULL
);

-- ============================================================================
-- Verification block — what this file did
-- ============================================================================
-- Tables created                : 2   (attendance, class_teacher)
-- Foreign keys added            : 4
--   Within-section              : 0
--   Cross-section to bootstrap  : 1   (class_teacher.academic_year_id)
--   Cross-section to academic_structure : 1 (class_teacher.section_id)
--   Cross-section to students   : 1   (attendance.enrollment_id)
--   Cross-section to hr_management : 1 (class_teacher.staff_id)
-- Junction tables               : 0
-- Circular FKs                  : 0
-- ALTER TABLE statements        : 2   (attendance: add source column + index)
-- Unique constraints            : 2   (attendance: enrollment+date;
--                                      class_teacher: section+year)
-- DB FKs added that weren't @ManyToOne in Java : 1
--   (attendance.enrollment_id — plain @Column, added FK for integrity)
-- ============================================================================
