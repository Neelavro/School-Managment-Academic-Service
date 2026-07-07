-- ============================================================================
-- Migration: password_reset_tokens (forgot-password flow)
-- ============================================================================
-- Short-lived per-user 6-digit reset codes. Raw code is bcrypted here; the
-- plaintext is written to academic_service stdout until SMS/email provider
-- is wired up. TTL 10 minutes; enforced app-side.
-- ============================================================================

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
