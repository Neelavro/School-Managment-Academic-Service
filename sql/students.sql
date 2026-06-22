-- ============================================================================
-- academic_service — Students section
-- ============================================================================
-- Feature-specific tables for the "Students" sidebar items:
--   All Students, Archived Students, Student Transfer, 4th Subject Override
--
-- (Archived Students and Student Transfer are filtered views over the same
-- enrollment/student tables — not separate tables.)
--
-- Foundation tables used: gender, student_status, subject, academic_year,
-- shift, gender_section, student_group (all from bootstrap.sql).
-- Plus class + section from academic_structure.sql.
--
-- Circular dependency handling — Student and StudentImage reference each other:
--   student.image_id        →  student_image.id   (the "current image" pointer)
--   student_image.student_id →  student.id        (the "owner" back-reference)
-- We create both tables WITHOUT the cross-FKs, then ALTER both to add them.
--
-- All data columns are nullable. App layer enforces "required" rules.
-- Idempotent — uses CREATE TABLE IF NOT EXISTS.
--
-- Run order:
--   bootstrap.sql  →  academic_structure.sql  →  students.sql
-- ============================================================================

-- ── 1. student_image  (FK to student added later via ALTER) ────────────────
CREATE TABLE IF NOT EXISTS student_image (
  id          INT          NOT NULL AUTO_INCREMENT,
  image_url   VARCHAR(500)     NULL,
  is_active   BIT(1)           NULL DEFAULT b'1',
  created_at  DATETIME         NULL,
  student_id  BIGINT           NULL,
  PRIMARY KEY (id),
  KEY idx_student_image_student (student_id)
);

-- ── 2. student  (FK to student_image added later via ALTER) ────────────────
CREATE TABLE IF NOT EXISTS student (
  id                          BIGINT       NOT NULL AUTO_INCREMENT,
  student_system_id           VARCHAR(255)     NULL,
  name_bangla                 VARCHAR(255)     NULL,
  name_english                VARCHAR(255)     NULL,
  class_roll                  INT              NULL,
  -- Father's Info
  father_name_bangla          VARCHAR(255)     NULL,
  father_name_english         VARCHAR(255)     NULL,
  father_occupation           VARCHAR(255)     NULL,
  father_phone                VARCHAR(50)      NULL,
  father_monthly_salary       VARCHAR(50)      NULL,
  -- Mother's Info
  mother_name_bangla          VARCHAR(255)     NULL,
  mother_name_english         VARCHAR(255)     NULL,
  mother_occupation           VARCHAR(255)     NULL,
  mother_phone                VARCHAR(50)      NULL,
  mother_monthly_salary       VARCHAR(50)      NULL,
  -- Guardian Info
  guardian_name_bangla        VARCHAR(255)     NULL,
  guardian_name_english       VARCHAR(255)     NULL,
  guardian_occupation         VARCHAR(255)     NULL,
  guardian_phone              VARCHAR(50)      NULL,
  guardian_relation           VARCHAR(100)     NULL,
  -- Current Address
  current_holding_no          VARCHAR(255)     NULL,
  current_road_or_village     VARCHAR(255)     NULL,
  current_district            VARCHAR(255)     NULL,
  current_thana               VARCHAR(255)     NULL,
  -- Permanent Address
  permanent_holding_no        VARCHAR(255)     NULL,
  permanent_road_or_village   VARCHAR(255)     NULL,
  permanent_district          VARCHAR(255)     NULL,
  permanent_thana             VARCHAR(255)     NULL,
  dob                         DATE             NULL,
  nationality                 VARCHAR(100)     NULL,
  is_active                   BIT(1)           NULL,
  password_hash               VARCHAR(255)     NULL,
  gender_id                   INT              NULL,
  student_status_id           INT              NULL,
  image_id                    INT              NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_student_system_id (student_system_id),
  KEY idx_student_gender (gender_id),
  KEY idx_student_status (student_status_id),
  KEY idx_student_image (image_id),
  CONSTRAINT fk_student_gender
      FOREIGN KEY (gender_id)         REFERENCES gender(id)          ON DELETE SET NULL,
  CONSTRAINT fk_student_status
      FOREIGN KEY (student_status_id) REFERENCES student_status(id)  ON DELETE SET NULL
);

-- ── 3. Add the cross-FKs now that both tables exist ────────────────────────
-- student_image.student_id  →  student.id
-- student.image_id          →  student_image.id
ALTER TABLE student_image ADD CONSTRAINT fk_student_image_student
    FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE;

ALTER TABLE student ADD CONSTRAINT fk_student_image
    FOREIGN KEY (image_id) REFERENCES student_image(id) ON DELETE SET NULL;

-- ── 4. enrollment  (the per-academic-year placement of a student) ──────────
-- student_system_id is BOTH the FK to student.student_system_id AND a regular
-- column (JPA uses this dual-role pattern). The FK enforces existence.
CREATE TABLE IF NOT EXISTS enrollment (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  student_system_id   VARCHAR(255)     NULL,
  academic_year_id    INT              NULL,
  class_id            INT              NULL,
  section_id          BIGINT           NULL,
  shift_id            INT              NULL,
  gender_section_id   INT              NULL,
  student_group_id    INT              NULL,
  class_roll          INT              NULL,
  is_active           BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id),
  KEY idx_enrollment_class_active (class_id, is_active),
  KEY idx_enrollment_year_class_active (academic_year_id, class_id, is_active),
  KEY idx_enrollment_sysid_active (student_system_id, is_active),
  KEY idx_enrollment_section (section_id),
  KEY idx_enrollment_shift (shift_id),
  KEY idx_enrollment_gender_section (gender_section_id),
  KEY idx_enrollment_student_group (student_group_id),
  CONSTRAINT fk_enrollment_student
      FOREIGN KEY (student_system_id) REFERENCES student(student_system_id) ON DELETE RESTRICT,
  CONSTRAINT fk_enrollment_year
      FOREIGN KEY (academic_year_id)  REFERENCES academic_year(id)          ON DELETE SET NULL,
  CONSTRAINT fk_enrollment_class
      FOREIGN KEY (class_id)          REFERENCES class(id)                  ON DELETE SET NULL,
  CONSTRAINT fk_enrollment_section
      FOREIGN KEY (section_id)        REFERENCES section(id)                ON DELETE SET NULL,
  CONSTRAINT fk_enrollment_shift
      FOREIGN KEY (shift_id)          REFERENCES shift(id)                  ON DELETE SET NULL,
  CONSTRAINT fk_enrollment_gender_section
      FOREIGN KEY (gender_section_id) REFERENCES gender_section(id)         ON DELETE SET NULL,
  CONSTRAINT fk_enrollment_student_group
      FOREIGN KEY (student_group_id)  REFERENCES student_group(id)          ON DELETE SET NULL
);

-- ── 5. student_fourth_subject_override  (per-enrollment fourth-subject pick) ─
CREATE TABLE IF NOT EXISTS student_fourth_subject_override (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  enrollment_id   BIGINT           NULL,
  subject_id      INT              NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_fourth_override_enrollment (enrollment_id),
  KEY idx_fourth_override_subject (subject_id),
  CONSTRAINT fk_fourth_override_enrollment
      FOREIGN KEY (enrollment_id) REFERENCES enrollment(id) ON DELETE CASCADE,
  CONSTRAINT fk_fourth_override_subject
      FOREIGN KEY (subject_id)    REFERENCES subject(id)    ON DELETE SET NULL
);

-- ── 6. student_fourth_subject_compulsory  (junction: override ↔ subject) ───
-- Records the compulsory subjects associated with a fourth-subject override.
CREATE TABLE IF NOT EXISTS student_fourth_subject_compulsory (
  override_id   BIGINT       NOT NULL,
  subject_id    INT          NOT NULL,
  PRIMARY KEY (override_id, subject_id),
  KEY idx_fourth_compulsory_subject (subject_id),
  CONSTRAINT fk_fourth_compulsory_override
      FOREIGN KEY (override_id) REFERENCES student_fourth_subject_override(id) ON DELETE CASCADE,
  CONSTRAINT fk_fourth_compulsory_subject
      FOREIGN KEY (subject_id)  REFERENCES subject(id)                         ON DELETE RESTRICT
);
