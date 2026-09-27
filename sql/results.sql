-- ============================================================================
-- academic_service — Results section
-- ============================================================================
-- The "Results" sidebar section has 6 items:
--   Session Results, Routine Results, Annual Results, Merit List,
--   Statistics, Publish Results
--
-- "Publish Results" is backed by result_publication, plus published_result
-- (each student's stored result, written on publish and read by the student
-- portal) and result_publish_job (the publish queue). The other five are COMPUTED VIEWS over student_mark,
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

-- ── result_publication  (one row per routine × class — is it published?) ──
-- Matches the ResultPublication entity and the live databases.
CREATE TABLE IF NOT EXISTS result_publication (
  id            INT          NOT NULL AUTO_INCREMENT,
  routine_id    INT              NULL,
  class_id      INT          NOT NULL,
  published     BIT(1)           NULL DEFAULT b'0',
  published_at  DATETIME         NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_result_pub_routine_class (routine_id, class_id),
  CONSTRAINT fk_result_pub_routine FOREIGN KEY (routine_id)
      REFERENCES exam_routine(id) ON DELETE CASCADE,
  CONSTRAINT fk_rp_class FOREIGN KEY (class_id)
      REFERENCES class(id)
);

-- ── published_result  (one row per student per published routine) ────────
CREATE TABLE IF NOT EXISTS published_result (
  id                 BIGINT        NOT NULL AUTO_INCREMENT,
  routine_id         INT           NOT NULL,
  class_id           INT           NOT NULL,
  enrollment_id      BIGINT        NOT NULL,
  student_system_id  VARCHAR(255)  NOT NULL,
  total_marks        DECIMAL(10,2)     NULL,
  gpa                DOUBLE            NULL,
  passed             BIT(1)            NULL,
  result_json        MEDIUMTEXT    NOT NULL,   -- the portal's response body, stored ready to send
  computed_at        DATETIME      NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_published_result_enrollment_routine (enrollment_id, routine_id),
  KEY idx_published_result_student_routine (student_system_id, routine_id),
  KEY idx_published_result_routine_class (routine_id, class_id)
);

-- ── result_publish_job  (one row per "publish this class" request) ────────
-- Processed one at a time by ResultPublishService. status:
--   QUEUED → PROCESSING → DONE | FAILED
CREATE TABLE IF NOT EXISTS result_publish_job (
  id           BIGINT         NOT NULL AUTO_INCREMENT,
  routine_id   INT            NOT NULL,
  class_id     INT            NOT NULL,
  status       VARCHAR(20)    NOT NULL,
  total        INT                NULL,
  done         INT                NULL,
  error        VARCHAR(1000)      NULL,
  created_at   DATETIME       NOT NULL,
  started_at   DATETIME           NULL,
  finished_at  DATETIME           NULL,
  PRIMARY KEY (id),
  KEY idx_result_publish_job_routine (routine_id, class_id),
  KEY idx_result_publish_job_status (status)
);

-- ============================================================================
-- Verification block — what this file did
-- ============================================================================
-- Tables created                : 1   (result_publication)
-- Foreign keys added            : 1   (routine_id → exam_routine, cross-section)
-- Junction tables               : 0
-- Circular FKs                  : 0
-- ALTER TABLE statements        : 0
-- Unique constraints            : (routine_id, class_id); see the tables above
-- Skipped @Transient fields     : 0
-- Soft-delete columns           : 0
--
-- Sidebar items WITHOUT a backing table (computed views in app code):
--   Session Results, Routine Results, Annual Results, Merit List, Statistics
-- ============================================================================
