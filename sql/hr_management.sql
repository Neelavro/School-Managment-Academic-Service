-- ============================================================================
-- academic_service — HR Management section
-- ============================================================================
-- Feature-specific tables for the "HR Management" sidebar items:
--   Staff, Designations, Leave Types, Leave Requests, Approval Chains,
--   Teacher Duties
-- Plus all supporting tables (staff_document, leave_policy, etc.) that the
-- HR UI uses but aren't sidebar items themselves.
--
-- Foundation tables used:
--   gender                                 (bootstrap.sql)
--   academic_year, gender_section,
--     student_group, subject, room         (bootstrap.sql)
--   class, section, class_routine          (academic_structure.sql)
--   exam_session                           (exams.sql)
--   fbac_role                              (user_management.sql)  ⚠ CROSS-SECTION
--
-- ⚠ This file references fbac_role from User Management. Run user_management.sql
--   BEFORE hr_management.sql, OR drop the two FKs noted below as TODO.
--
-- Java enums stored as VARCHAR(20) columns (no tables):
--   ContractType, EmployeeType, QualificationLevel, LeaveStatus, ApprovalAction
--
-- All data columns are nullable. App layer enforces "required" rules.
-- Idempotent — uses CREATE TABLE IF NOT EXISTS.
--
-- Run order:
--   bootstrap.sql → academic_structure.sql → exams.sql → students.sql
--                → marking.sql → user_management.sql → hr_management.sql
-- ============================================================================

-- ── 1. leave_type  (independent) ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS leave_type (
  id            INT          NOT NULL AUTO_INCREMENT,
  name          VARCHAR(255)     NULL,
  annual_quota  INT              NULL,
  is_active     BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id),
  UNIQUE KEY uq_leave_type_name (name)
);

-- ── 2. designation  (independent) ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS designation (
  id            INT          NOT NULL AUTO_INCREMENT,
  name          VARCHAR(255)     NULL,
  description   VARCHAR(255)     NULL,
  is_active     BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id),
  UNIQUE KEY uq_designation_name (name)
);

-- ── 3. staff_duty_role  (independent — "Class Teacher", "Vice Principal") ─
CREATE TABLE IF NOT EXISTS staff_duty_role (
  id            INT          NOT NULL AUTO_INCREMENT,
  role_name     VARCHAR(255)     NULL,
  is_active     BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id),
  UNIQUE KEY uq_staff_duty_role_name (role_name)
);

-- ── 4. staff  (FK → gender bootstrap, current_designation_id same-section) ─
CREATE TABLE IF NOT EXISTS staff (
  id                          BIGINT       NOT NULL AUTO_INCREMENT,
  staff_system_id             VARCHAR(255)     NULL,
  -- Identity
  name_english                VARCHAR(255)     NULL,
  name_bangla                 VARCHAR(255)     NULL,
  father_name                 VARCHAR(255)     NULL,
  mother_name                 VARCHAR(255)     NULL,
  spouse_name                 VARCHAR(255)     NULL,
  dob                         DATE             NULL,
  gender_id                   INT              NULL,
  religion                    VARCHAR(100)     NULL,
  marital_status              VARCHAR(50)      NULL,
  blood_group                 VARCHAR(10)      NULL,
  nationality                 VARCHAR(100)     NULL,
  -- ID Documents
  national_id                 VARCHAR(50)      NULL,
  smart_card_number           VARCHAR(50)      NULL,
  birth_reg_number            VARCHAR(50)      NULL,
  -- Contact
  phone                       VARCHAR(50)      NULL,
  email                       VARCHAR(255)     NULL,
  -- Present address
  present_division            VARCHAR(100)     NULL,
  present_district            VARCHAR(100)     NULL,
  present_upazila             VARCHAR(100)     NULL,
  present_union               VARCHAR(100)     NULL,
  present_post_office         VARCHAR(100)     NULL,
  present_village             VARCHAR(255)     NULL,
  -- Permanent address
  permanent_division          VARCHAR(100)     NULL,
  permanent_district          VARCHAR(100)     NULL,
  permanent_upazila           VARCHAR(100)     NULL,
  permanent_union             VARCHAR(100)     NULL,
  permanent_post_office       VARCHAR(100)     NULL,
  permanent_village           VARCHAR(255)     NULL,
  -- Employment
  employee_type               VARCHAR(20)      NULL,            -- TEACHING | NON_TEACHING | ADMIN | SUPPORT
  contract_type               VARCHAR(20)      NULL,            -- PERMANENT | CONTRACTUAL | PART_TIME | PROBATIONARY
  department                  VARCHAR(255)     NULL,
  current_designation_id      INT              NULL,
  joining_date                DATE             NULL,
  -- Financial
  bank_name                   VARCHAR(255)     NULL,
  bank_branch                 VARCHAR(255)     NULL,
  bank_account_number         VARCHAR(50)      NULL,
  bank_routing_number         VARCHAR(50)      NULL,
  bkash_number                VARCHAR(50)      NULL,
  e_tin                       VARCHAR(50)      NULL,
  -- Status
  is_active                   BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id),
  UNIQUE KEY uq_staff_system_id (staff_system_id),
  KEY idx_staff_system_id (staff_system_id),
  KEY idx_staff_employee_type (employee_type),
  KEY idx_staff_active (is_active),
  KEY idx_staff_gender (gender_id),
  KEY idx_staff_designation (current_designation_id),
  CONSTRAINT fk_staff_gender FOREIGN KEY (gender_id)
      REFERENCES gender(id)      ON DELETE SET NULL,
  CONSTRAINT fk_staff_current_designation FOREIGN KEY (current_designation_id)
      REFERENCES designation(id) ON DELETE SET NULL
);

-- ── 5. designation_promotion  (which designations promote to which) ───────
CREATE TABLE IF NOT EXISTS designation_promotion (
  id                    INT          NOT NULL AUTO_INCREMENT,
  from_designation_id   INT              NULL,
  to_designation_id     INT              NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_dp_from_to (from_designation_id, to_designation_id),
  KEY idx_dp_from (from_designation_id),
  KEY idx_dp_to   (to_designation_id),
  CONSTRAINT fk_dp_from FOREIGN KEY (from_designation_id)
      REFERENCES designation(id) ON DELETE RESTRICT,
  CONSTRAINT fk_dp_to   FOREIGN KEY (to_designation_id)
      REFERENCES designation(id) ON DELETE RESTRICT
);

-- ── 6. staff_promotion  (audit log of staff promotions) ───────────────────
CREATE TABLE IF NOT EXISTS staff_promotion (
  id                    BIGINT       NOT NULL AUTO_INCREMENT,
  staff_id              BIGINT           NULL,
  from_designation_id   INT              NULL,
  to_designation_id     INT              NULL,
  promotion_date        DATE             NULL,
  notes                 TEXT             NULL,
  recorded_at           DATETIME         NULL,
  PRIMARY KEY (id),
  KEY idx_staff_promotion_staff (staff_id),
  KEY idx_sp_from (from_designation_id),
  KEY idx_sp_to   (to_designation_id),
  CONSTRAINT fk_sp_staff FOREIGN KEY (staff_id)
      REFERENCES staff(id)       ON DELETE CASCADE,
  CONSTRAINT fk_sp_from  FOREIGN KEY (from_designation_id)
      REFERENCES designation(id) ON DELETE SET NULL,
  CONSTRAINT fk_sp_to    FOREIGN KEY (to_designation_id)
      REFERENCES designation(id) ON DELETE SET NULL
);

-- ── 7. academic_qualification  (FK → staff) ───────────────────────────────
CREATE TABLE IF NOT EXISTS academic_qualification (
  id                    BIGINT       NOT NULL AUTO_INCREMENT,
  staff_id              BIGINT           NULL,
  level                 VARCHAR(30)      NULL,             -- SSC_DAKHIL | HSC_ALIM | BACHELOR_FAZIL | MASTERS_KAMIL | HIFZ_QIRAT | DAURA_TAKMIL | OTHER
  institute_name        VARCHAR(255)     NULL,
  board                 VARCHAR(255)     NULL,
  group_or_subject      VARCHAR(255)     NULL,
  passing_year          INT              NULL,
  result                VARCHAR(50)      NULL,
  roll_number           VARCHAR(50)      NULL,
  registration_number   VARCHAR(50)      NULL,
  sanad_details         TEXT             NULL,
  PRIMARY KEY (id),
  KEY idx_aq_staff (staff_id),
  CONSTRAINT fk_aq_staff FOREIGN KEY (staff_id)
      REFERENCES staff(id) ON DELETE CASCADE
);

-- ── 7b. teacher_profile  (per-staff MPO / NTRCA / teaching history) ───────
-- @OneToOne with staff via staff_id UNIQUE — one profile per staff.
CREATE TABLE IF NOT EXISTS teacher_profile (
  id                      BIGINT       NOT NULL AUTO_INCREMENT,
  staff_id                BIGINT           NULL,
  mpo_status              VARCHAR(20)      NULL,        -- MPO_LISTED | NON_MPO
  mpo_index_number        VARCHAR(255)     NULL,
  ntrca_reg_number        VARCHAR(255)     NULL,
  ntrca_cycle             VARCHAR(255)     NULL,
  years_of_experience     INT              NULL,
  previous_institutions   TEXT             NULL,
  trainings_completed     TEXT             NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_teacher_profile_staff (staff_id),
  CONSTRAINT fk_tp_staff FOREIGN KEY (staff_id)
      REFERENCES staff(id) ON DELETE CASCADE
);

-- ── 8. employment_history  (prior jobs) ───────────────────────────────────
CREATE TABLE IF NOT EXISTS employment_history (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  staff_id            BIGINT           NULL,
  organization_name   VARCHAR(255)     NULL,
  designation         VARCHAR(255)     NULL,
  from_date           VARCHAR(50)      NULL,             -- stored as String in entity
  to_date             VARCHAR(50)      NULL,             -- null = currently working
  job_type            VARCHAR(50)      NULL,
  responsibilities    VARCHAR(1000)    NULL,
  PRIMARY KEY (id),
  KEY idx_eh_staff (staff_id),
  CONSTRAINT fk_eh_staff FOREIGN KEY (staff_id)
      REFERENCES staff(id) ON DELETE CASCADE
);

-- ── 9. staff_document  (uploaded files per staff) ─────────────────────────
CREATE TABLE IF NOT EXISTS staff_document (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  staff_id        BIGINT           NULL,
  document_type   VARCHAR(100)     NULL,
  file_url        VARCHAR(500)     NULL,
  uploaded_at     DATETIME         NULL,
  PRIMARY KEY (id),
  KEY idx_staff_document_staff (staff_id),
  CONSTRAINT fk_sd_staff FOREIGN KEY (staff_id)
      REFERENCES staff(id) ON DELETE CASCADE
);

-- ── 10. staff_emergency_contact  (per-staff emergency contacts) ───────────
CREATE TABLE IF NOT EXISTS staff_emergency_contact (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  staff_id        BIGINT           NULL,
  name            VARCHAR(255)     NULL,
  relationship    VARCHAR(50)      NULL,
  phone           VARCHAR(50)      NULL,
  PRIMARY KEY (id),
  KEY idx_emergency_contact_staff (staff_id),
  CONSTRAINT fk_sec_staff FOREIGN KEY (staff_id)
      REFERENCES staff(id) ON DELETE CASCADE
);

-- ── 11. staff_dependent  (spouse/children/parents per staff) ──────────────
CREATE TABLE IF NOT EXISTS staff_dependent (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  staff_id        BIGINT           NULL,
  name            VARCHAR(255)     NULL,
  relationship    VARCHAR(50)      NULL,             -- SPOUSE | SON | DAUGHTER | PARENT
  dob             DATE             NULL,
  nid_number      VARCHAR(50)      NULL,
  PRIMARY KEY (id),
  KEY idx_staff_dependent_staff (staff_id),
  CONSTRAINT fk_sdep_staff FOREIGN KEY (staff_id)
      REFERENCES staff(id) ON DELETE CASCADE
);

-- ── 12. staff_class_assignment  (staff teaching X subject in Y class) ─────
-- Many cross-section FKs (year, class, gender_section, section, group, subject)
CREATE TABLE IF NOT EXISTS staff_class_assignment (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  staff_id            BIGINT           NULL,
  academic_year_id    INT              NULL,
  class_id            INT              NULL,
  gender_section_id   INT              NULL,
  section_id          BIGINT           NULL,
  student_group_id    INT              NULL,
  subject_id          INT              NULL,
  PRIMARY KEY (id),
  KEY idx_sca_staff_year (staff_id, academic_year_id),
  KEY idx_sca_class_year (class_id, academic_year_id),
  CONSTRAINT fk_sca_staff    FOREIGN KEY (staff_id)
      REFERENCES staff(id)            ON DELETE CASCADE,
  CONSTRAINT fk_sca_year     FOREIGN KEY (academic_year_id)
      REFERENCES academic_year(id)    ON DELETE SET NULL,
  CONSTRAINT fk_sca_class    FOREIGN KEY (class_id)
      REFERENCES class(id)            ON DELETE SET NULL,
  CONSTRAINT fk_sca_gsection FOREIGN KEY (gender_section_id)
      REFERENCES gender_section(id)   ON DELETE SET NULL,
  CONSTRAINT fk_sca_section  FOREIGN KEY (section_id)
      REFERENCES section(id)          ON DELETE SET NULL,
  CONSTRAINT fk_sca_group    FOREIGN KEY (student_group_id)
      REFERENCES student_group(id)    ON DELETE SET NULL,
  CONSTRAINT fk_sca_subject  FOREIGN KEY (subject_id)
      REFERENCES subject(id)          ON DELETE SET NULL
);

-- ── 13. staff_duty_role_assignment  (which staff has which duty role) ─────
CREATE TABLE IF NOT EXISTS staff_duty_role_assignment (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  staff_id      BIGINT           NULL,
  duty_role_id  INT              NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_sdra_staff_role (staff_id, duty_role_id),
  KEY idx_duty_role_assignment_staff (staff_id),
  CONSTRAINT fk_sdra_staff FOREIGN KEY (staff_id)
      REFERENCES staff(id)            ON DELETE CASCADE,
  CONSTRAINT fk_sdra_role  FOREIGN KEY (duty_role_id)
      REFERENCES staff_duty_role(id)  ON DELETE RESTRICT
);

-- ── 14. teacher_exam_duty  (invigilation assignments) ─────────────────────
CREATE TABLE IF NOT EXISTS teacher_exam_duty (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  staff_id          BIGINT           NULL,
  exam_session_id   INT              NULL,
  room_id           INT              NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_ted_staff_session (staff_id, exam_session_id),
  KEY idx_ted_staff (staff_id),
  KEY idx_ted_session_room (exam_session_id, room_id),
  CONSTRAINT fk_ted_staff   FOREIGN KEY (staff_id)
      REFERENCES staff(id)         ON DELETE CASCADE,
  CONSTRAINT fk_ted_session FOREIGN KEY (exam_session_id)
      REFERENCES exam_session(id)  ON DELETE CASCADE,
  CONSTRAINT fk_ted_room    FOREIGN KEY (room_id)
      REFERENCES room(id)          ON DELETE SET NULL
);

-- ── 15. teacher_period_duty  (which teacher teaches which routine period) ─
-- UNIQUE on class_routine_id — only one teacher per routine slot.
CREATE TABLE IF NOT EXISTS teacher_period_duty (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  staff_id          BIGINT           NULL,
  class_routine_id  INT              NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_tpd_routine (class_routine_id),
  KEY idx_tpd_staff (staff_id),
  KEY idx_tpd_routine (class_routine_id),
  CONSTRAINT fk_tpd_staff   FOREIGN KEY (staff_id)
      REFERENCES staff(id)         ON DELETE CASCADE,
  CONSTRAINT fk_tpd_routine FOREIGN KEY (class_routine_id)
      REFERENCES class_routine(id) ON DELETE CASCADE
);

-- ── 16. leave_policy  (annual days allowed per designation × leave_type) ──
CREATE TABLE IF NOT EXISTS leave_policy (
  id              INT          NOT NULL AUTO_INCREMENT,
  designation_id  INT              NULL,
  leave_type_id   INT              NULL,
  annual_days     INT              NULL,
  is_active       BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id),
  UNIQUE KEY uq_lp_designation_type (designation_id, leave_type_id),
  KEY idx_leave_policy_designation (designation_id),
  CONSTRAINT fk_lp_designation FOREIGN KEY (designation_id)
      REFERENCES designation(id) ON DELETE CASCADE,
  CONSTRAINT fk_lp_leave_type  FOREIGN KEY (leave_type_id)
      REFERENCES leave_type(id)  ON DELETE RESTRICT
);

-- ── 17. leave_request  (each leave application) ──────────────────────────
CREATE TABLE IF NOT EXISTS leave_request (
  id                    BIGINT       NOT NULL AUTO_INCREMENT,
  staff_id              BIGINT           NULL,
  leave_type_id         INT              NULL,
  start_date            DATE             NULL,
  end_date              DATE             NULL,
  reason                TEXT             NULL,
  total_days            INT              NULL DEFAULT 0,
  status                VARCHAR(30)      NULL DEFAULT 'PENDING',  -- PENDING | PARTIALLY_APPROVED | APPROVED | REJECTED
  current_tier_order    INT              NULL DEFAULT 1,
  submitted_at          DATETIME         NULL,
  PRIMARY KEY (id),
  KEY idx_leave_request_staff (staff_id),
  KEY idx_leave_request_status (status),
  KEY idx_leave_request_type (leave_type_id),
  CONSTRAINT fk_lr_staff      FOREIGN KEY (staff_id)
      REFERENCES staff(id)       ON DELETE CASCADE,
  CONSTRAINT fk_lr_leave_type FOREIGN KEY (leave_type_id)
      REFERENCES leave_type(id)  ON DELETE RESTRICT
);

-- ── 18. leave_balance  (per-staff per-type per-year balances) ─────────────
CREATE TABLE IF NOT EXISTS leave_balance (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  staff_id        BIGINT           NULL,
  leave_type_id   INT              NULL,
  year            INT              NULL,
  allocated_days  INT              NULL DEFAULT 0,
  used_days       INT              NULL DEFAULT 0,
  pending_days    INT              NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uq_lb_staff_type_year (staff_id, leave_type_id, year),
  KEY idx_leave_balance_staff (staff_id),
  KEY idx_leave_balance_year (year),
  CONSTRAINT fk_lb_staff      FOREIGN KEY (staff_id)
      REFERENCES staff(id)       ON DELETE CASCADE,
  CONSTRAINT fk_lb_leave_type FOREIGN KEY (leave_type_id)
      REFERENCES leave_type(id)  ON DELETE RESTRICT
);

-- ── 19. leave_approval_record  (one row per approval tier action) ─────────
CREATE TABLE IF NOT EXISTS leave_approval_record (
  id                    BIGINT       NOT NULL AUTO_INCREMENT,
  leave_request_id      BIGINT           NULL,
  tier_order            INT              NULL,
  approved_by_user_id   BIGINT           NULL,
  action                VARCHAR(20)      NULL,             -- APPROVED | REJECTED
  remarks               VARCHAR(500)     NULL,
  action_at             DATETIME         NULL,
  PRIMARY KEY (id),
  KEY idx_leave_approval_request (leave_request_id),
  CONSTRAINT fk_lar_request FOREIGN KEY (leave_request_id)
      REFERENCES leave_request(id) ON DELETE CASCADE
);

-- ── 20. leave_approval_tier  (FK → fbac_role CROSS-SECTION) ───────────────
-- ⚠ fbac_role lives in user_management.sql. Run that file BEFORE this one.
CREATE TABLE IF NOT EXISTS leave_approval_tier (
  id            INT          NOT NULL AUTO_INCREMENT,
  leave_type_id INT              NULL,
  tier_order    INT              NULL,
  tier_label    VARCHAR(255)     NULL,
  fbac_role_id  INT              NULL,
  PRIMARY KEY (id),
  KEY idx_leave_tier_type (leave_type_id),
  KEY idx_leave_tier_role (fbac_role_id),
  CONSTRAINT fk_lat_leave_type FOREIGN KEY (leave_type_id)
      REFERENCES leave_type(id) ON DELETE CASCADE,
  CONSTRAINT fk_lat_fbac_role  FOREIGN KEY (fbac_role_id)
      REFERENCES fbac_role(id)  ON DELETE SET NULL
);

-- ── 21. designation_approval_chain  (FK → fbac_role CROSS-SECTION) ────────
-- ⚠ Same fbac_role cross-section dependency as above.
CREATE TABLE IF NOT EXISTS designation_approval_chain (
  id                INT          NOT NULL AUTO_INCREMENT,
  designation_id    INT              NULL,
  tier_order        INT              NULL,
  tier_label        VARCHAR(255)     NULL,
  approver_role_id  INT              NULL,
  PRIMARY KEY (id),
  KEY idx_dac_designation (designation_id),
  KEY idx_dac_role (approver_role_id),
  CONSTRAINT fk_dac_designation FOREIGN KEY (designation_id)
      REFERENCES designation(id) ON DELETE CASCADE,
  CONSTRAINT fk_dac_fbac_role   FOREIGN KEY (approver_role_id)
      REFERENCES fbac_role(id)   ON DELETE SET NULL
);

-- ── Deferred FK from user_management.sql ──────────────────────────────────
-- system_user.staff_id couldn't be FK'd at UM time because staff didn't exist.
-- Now both tables exist, so wire it up here.
ALTER TABLE system_user ADD CONSTRAINT fk_system_user_staff
    FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE SET NULL;

-- ============================================================================
-- Verification block — what this file did
-- ============================================================================
-- Tables created                : 22  (added teacher_profile)
-- Foreign keys added            : 39
--   Within-section              : 25  (added teacher_profile.staff_id)
--   Cross-section to bootstrap  :  7  (gender, academic_year, gender_section,
--                                       student_group, subject ×2, room)
--   Cross-section to academic_structure : 3 (class, section, class_routine)
--   Cross-section to exams      :  1  (exam_session)
--   Cross-section to user_management : 2 (fbac_role × 2)   ⚠ REQUIRES UM FIRST
--   Deferred FK from UM         :  1  (system_user.staff_id → staff)
-- Junction tables               : 0
-- Circular FKs                  : 0
-- ALTER TABLE statements        : 1  (deferred system_user.staff_id FK)
-- Unique constraints            : 11  (added teacher_profile.staff_id UNIQUE)
--   leave_type.name, designation.name, staff_duty_role.role_name,
--   staff.staff_system_id, designation_promotion 2-col,
--   staff_duty_role_assignment 2-col, teacher_exam_duty 2-col,
--   teacher_period_duty 1-col, leave_policy 2-col, leave_balance 3-col
-- Skipped @Transient fields     : 0
-- Java enums stored as VARCHAR  : 5  (ContractType, EmployeeType,
--                                     QualificationLevel, LeaveStatus,
--                                     ApprovalAction)
-- Total entities reviewed       : 27  (22 @Entity classes including teacher_profile + 5 enum classes)
-- Additional enum stored        : MpoStatus (MPO_LISTED | NON_MPO) on teacher_profile.mpo_status
-- ============================================================================
