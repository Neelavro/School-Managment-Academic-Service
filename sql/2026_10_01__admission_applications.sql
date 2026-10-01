-- ============================================================================
-- Online admission applications
-- ============================================================================
-- One row per form submitted on the public online-admission page (same fields
-- as the institution's old EduZen form). Applications wait as PENDING until an
-- admin approves or rejects them in the panel. Nothing here touches students
-- or enrollments yet.
--
-- Idempotent — safe to re-run.
-- ============================================================================

CREATE TABLE IF NOT EXISTS admission_application (
  id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
  application_no         VARCHAR(20)  NULL,
  academic_year_id       INT          NULL,
  class_id               INT          NOT NULL,
  shift_id               INT          NULL,
  student_group_id       INT          NULL,
  category               VARCHAR(30)  NOT NULL,

  applicant_name         VARCHAR(150) NOT NULL,
  gender                 VARCHAR(10)  NOT NULL,
  religion               VARCHAR(20)  NOT NULL,
  dob                    DATE         NOT NULL,
  blood_group            VARCHAR(5)   NULL,
  nationality            VARCHAR(20)  NOT NULL,
  birth_certificate_no   VARCHAR(17)  NULL,
  quota                  VARCHAR(40)  NULL,
  photo_url              VARCHAR(500) NULL,

  father_name            VARCHAR(150) NOT NULL,
  father_mobile          VARCHAR(11)  NOT NULL,
  father_nid             VARCHAR(17)  NOT NULL,
  father_occupation      VARCHAR(40)  NULL,
  father_education       VARCHAR(150) NULL,
  father_income          VARCHAR(20)  NULL,

  mother_name            VARCHAR(150) NOT NULL,
  mother_mobile          VARCHAR(11)  NOT NULL,
  mother_nid             VARCHAR(17)  NOT NULL,
  mother_occupation      VARCHAR(40)  NULL,
  mother_education       VARCHAR(150) NULL,
  mother_income          VARCHAR(20)  NULL,

  present_address        VARCHAR(1000) NOT NULL,
  permanent_address      VARCHAR(1000) NOT NULL,

  guardian_type          VARCHAR(10)  NOT NULL,
  guardian_name          VARCHAR(150) NOT NULL,
  guardian_relation      VARCHAR(50)  NOT NULL,
  guardian_mobile        VARCHAR(11)  NOT NULL,
  guardian_occupation    VARCHAR(100) NULL,

  last_institute_name    VARCHAR(200) NULL,
  last_class_name        VARCHAR(100) NULL,
  previous_roll          VARCHAR(10)  NULL,
  previous_gpa           VARCHAR(4)   NULL,

  status                 VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
  review_note            VARCHAR(500) NULL,
  reviewed_by            VARCHAR(50)  NULL,
  reviewed_at            DATETIME     NULL,
  created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uk_admission_application_no (application_no),
  KEY idx_admission_status_created (status, created_at),
  KEY idx_admission_class (class_id),
  CONSTRAINT fk_admission_class FOREIGN KEY (class_id) REFERENCES class(id),
  CONSTRAINT fk_admission_year FOREIGN KEY (academic_year_id) REFERENCES academic_year(id) ON DELETE SET NULL,
  CONSTRAINT fk_admission_shift FOREIGN KEY (shift_id) REFERENCES shift(id) ON DELETE SET NULL,
  CONSTRAINT fk_admission_group FOREIGN KEY (student_group_id) REFERENCES student_group(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
