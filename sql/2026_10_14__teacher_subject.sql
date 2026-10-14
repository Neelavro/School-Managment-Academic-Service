-- ============================================================================
-- Subject teachers (LRDM)
-- ============================================================================
-- Which teacher teaches which subject to a class in an academic year, without a
-- class routine. The teacher portal's mark entry (/teacher-duties/me/mark-assignments,
-- /teacher-marks/*) follows these rows instead of routine periods. Teacher Duties
-- still assigns routine periods (teacher_period_duty is unchanged).
-- gender_section_id / section_id / student_group_id NULL = the whole class (or all).
--
-- Idempotent — safe to re-run. Creates an empty table; no data is copied.
-- ============================================================================

CREATE TABLE IF NOT EXISTS teacher_subject (
  id                BIGINT      NOT NULL AUTO_INCREMENT,
  staff_id          BIGINT      NOT NULL,
  academic_year_id  INT         NOT NULL,
  class_id          INT         NOT NULL,
  subject_id        INT         NOT NULL,
  gender_section_id INT             NULL,
  section_id        BIGINT          NULL,
  student_group_id  INT             NULL,
  created_at        DATETIME(6)     NULL,
  PRIMARY KEY (id),
  KEY idx_ts_staff_year (staff_id, academic_year_id),
  KEY idx_ts_class_subject (class_id, subject_id, academic_year_id),
  CONSTRAINT fk_ts_staff          FOREIGN KEY (staff_id)          REFERENCES staff(id)          ON DELETE CASCADE,
  CONSTRAINT fk_ts_academic_year  FOREIGN KEY (academic_year_id)  REFERENCES academic_year(id)  ON DELETE CASCADE,
  CONSTRAINT fk_ts_class          FOREIGN KEY (class_id)          REFERENCES class(id)          ON DELETE CASCADE,
  CONSTRAINT fk_ts_subject        FOREIGN KEY (subject_id)        REFERENCES subject(id)        ON DELETE CASCADE,
  CONSTRAINT fk_ts_gender_section FOREIGN KEY (gender_section_id) REFERENCES gender_section(id) ON DELETE CASCADE,
  CONSTRAINT fk_ts_section        FOREIGN KEY (section_id)        REFERENCES section(id)        ON DELETE CASCADE,
  CONSTRAINT fk_ts_student_group  FOREIGN KEY (student_group_id)  REFERENCES student_group(id)  ON DELETE CASCADE
) ENGINE=InnoDB;
