-- ============================================================================
-- academic_service — User Management section
-- ============================================================================
-- Feature-specific tables for the "User Management" sidebar items:
--   Access Roles, System Users, Audit Logs
--
-- Foundation tables used: none from bootstrap.
--
-- Cross-section deferred FK:
--   system_user.staff_id  →  staff(id)   (HR Management)
--   This FK is added at the END of hr_management.sql once both tables exist.
--
-- Java enums stored as VARCHAR (no tables):
--   UserType, AuditActionType, Submodule
--
-- All data columns are nullable. App layer enforces "required" rules.
-- Idempotent — uses CREATE TABLE IF NOT EXISTS.
--
-- Run order:
--   bootstrap.sql → ... → user_management.sql → hr_management.sql → ...
-- ============================================================================

-- ── 1. fbac_role  (independent — named permission bundles) ────────────────
CREATE TABLE IF NOT EXISTS fbac_role (
  id            INT          NOT NULL AUTO_INCREMENT,
  role_name     VARCHAR(255)     NULL,
  description   VARCHAR(255)     NULL,
  is_active     BIT(1)           NULL DEFAULT b'1',
  PRIMARY KEY (id),
  UNIQUE KEY uq_fbac_role_name (role_name)
);

-- ── 2. system_user  (independent — login accounts) ────────────────────────
-- staff_id has NO FK at this point — it'll be added in hr_management.sql.
CREATE TABLE IF NOT EXISTS system_user (
  id                      BIGINT       NOT NULL AUTO_INCREMENT,
  phone                   VARCHAR(50)      NULL,
  password_hash           VARCHAR(255)     NULL,
  user_type               VARCHAR(20)      NULL,      -- SUPER_ADMIN | ADMIN | TEACHER | STUDENT
  staff_id                BIGINT           NULL,      -- FK added in hr_management.sql
  is_active               BIT(1)           NULL DEFAULT b'1',
  is_suspended            BIT(1)           NULL DEFAULT b'0',
  must_reset_password     BIT(1)           NULL DEFAULT b'0',
  last_login_at           DATETIME         NULL,
  created_at              DATETIME         NULL,
  has_teacher_portal      BIT(1)           NULL DEFAULT b'0',
  has_admin_portal        BIT(1)           NULL DEFAULT b'0',
  PRIMARY KEY (id),
  UNIQUE KEY uq_system_user_phone (phone),
  KEY idx_system_user_phone (phone),
  KEY idx_system_user_staff (staff_id),
  KEY idx_system_user_type (user_type)
);

-- ── 3. fbac_permission  (permission entries per role × submodule) ─────────
-- UNIQUE on (fbac_role_id, submodule) — one permission row per pair.
CREATE TABLE IF NOT EXISTS fbac_permission (
  id            INT          NOT NULL AUTO_INCREMENT,
  fbac_role_id  INT              NULL,
  submodule     VARCHAR(50)      NULL,      -- e.g. ACCOUNTS_INVOICES, HR_STAFF, STUDENTS  (see Submodule enum)
  can_create    BIT(1)           NULL DEFAULT b'0',
  can_read      BIT(1)           NULL DEFAULT b'0',
  can_update    BIT(1)           NULL DEFAULT b'0',
  can_delete    BIT(1)           NULL DEFAULT b'0',
  PRIMARY KEY (id),
  UNIQUE KEY uq_fbac_permission_role_submodule (fbac_role_id, submodule),
  KEY idx_fbac_permission_role (fbac_role_id),
  CONSTRAINT fk_fbac_permission_role FOREIGN KEY (fbac_role_id)
      REFERENCES fbac_role(id) ON DELETE CASCADE
);

-- ── 4. system_user_role  (which users have which roles — M2M-ish) ─────────
-- UNIQUE on (system_user_id, fbac_role_id) — one row per user × role pair.
CREATE TABLE IF NOT EXISTS system_user_role (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  system_user_id  BIGINT           NULL,
  fbac_role_id    INT              NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_sur_user_role (system_user_id, fbac_role_id),
  KEY idx_user_role_user (system_user_id),
  KEY idx_user_role_role (fbac_role_id),
  CONSTRAINT fk_sur_user FOREIGN KEY (system_user_id)
      REFERENCES system_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_sur_role FOREIGN KEY (fbac_role_id)
      REFERENCES fbac_role(id)   ON DELETE CASCADE
);

-- ── 5. audit_log  (immutable record of CREATE/UPDATE/DELETE actions) ──────
-- user_id is plain Long (@Column) in Java but conceptually FKs to system_user.id.
-- DB FK added here for integrity, matching the pattern used in marking.sql.
CREATE TABLE IF NOT EXISTS audit_log (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  user_id       BIGINT           NULL,
  ip_address    VARCHAR(50)      NULL,
  action_type   VARCHAR(20)      NULL,      -- CREATE | UPDATE | DELETE
  submodule     VARCHAR(50)      NULL,      -- see Submodule enum
  entity_type   VARCHAR(100)     NULL,
  entity_id     VARCHAR(100)     NULL,
  description   TEXT             NULL,
  before_value  LONGTEXT         NULL,
  after_value   LONGTEXT         NULL,
  timestamp     DATETIME         NULL,
  PRIMARY KEY (id),
  KEY idx_audit_user (user_id),
  KEY idx_audit_timestamp (timestamp),
  KEY idx_audit_submodule (submodule),
  CONSTRAINT fk_audit_user FOREIGN KEY (user_id)
      REFERENCES system_user(id) ON DELETE SET NULL
);

-- ── 6. password_reset_tokens  (forgot-password 6-digit codes, TTL 10 min) ─
-- One row per user; overwritten on each new request. Raw code is bcrypted;
-- the plaintext is written to server stdout so a school admin can hand it
-- off out-of-band until SMS/email is wired up.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  system_user_id  BIGINT       NOT NULL,
  code_hash       VARCHAR(255) NOT NULL,
  created_at      DATETIME(6)  NOT NULL,
  expires_at      DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_prt_user (system_user_id),
  KEY idx_prt_expires_at (expires_at),
  CONSTRAINT fk_prt_user FOREIGN KEY (system_user_id)
      REFERENCES system_user(id) ON DELETE CASCADE
);

-- ============================================================================
-- Verification block — what this file did
-- ============================================================================
-- Tables created                : 5  (fbac_role, system_user, fbac_permission,
--                                      system_user_role, audit_log)
-- Foreign keys added            : 4  (3 within-section, 1 from audit_log)
-- Junction-like tables          : 1  (system_user_role — has its own id col but
--                                      acts as M2M between system_user & fbac_role)
-- Circular FKs                  : 0
-- Deferred FKs                  : 1  (system_user.staff_id → staff(id))
--                                     Added at the end of hr_management.sql.
-- ALTER TABLE statements        : 0
-- Unique constraints            : 4  (fbac_role.role_name, system_user.phone,
--                                      fbac_permission (role+submodule),
--                                      system_user_role (user+role))
-- Skipped @Transient fields     : 0
-- Java enums stored as VARCHAR  : 3  (UserType, AuditActionType, Submodule)
-- DB FKs added that weren't @ManyToOne in Java : 1
--   (audit_log.user_id — plain @Column, added FK for integrity)
-- ============================================================================
