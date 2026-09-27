-- ============================================================================
-- Stored results for the student portal
-- ============================================================================
-- Publishing a class's results now computes every student's result once and
-- stores it here; the student portal reads the stored row instead of
-- computing it on each request. Unpublishing deletes the class's rows.
-- Marks and marking structures are locked while a class is published.
--
-- Idempotent — safe to run on an existing database.
-- The app backfills rows for classes that were published before this table
-- existed (on startup), so no data migration is needed.
-- ============================================================================

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
