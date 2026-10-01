-- ============================================================================
-- Notices (website notice board)
-- ============================================================================
-- Written in the admin panel, read by the public school website. Bangla and
-- English text (either may be blank; the site falls back to the other), an
-- optional PDF/image attachment stored next to student photos, and a
-- published flag so drafts stay off the site.
--
-- Idempotent — safe to re-run.
-- ============================================================================

CREATE TABLE IF NOT EXISTS notice (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  title_bn          VARCHAR(300)  NULL,
  title_en          VARCHAR(300)  NULL,
  body_bn           TEXT          NULL,
  body_en           TEXT          NULL,
  notice_date       DATE          NOT NULL,
  category          VARCHAR(20)   NOT NULL DEFAULT 'general',
  pinned            BOOLEAN       NOT NULL DEFAULT FALSE,
  published         BOOLEAN       NOT NULL DEFAULT TRUE,
  attachment_file   VARCHAR(200)  NULL,
  attachment_name   VARCHAR(200)  NULL,
  attachment_type   VARCHAR(50)   NULL,
  attachment_size   BIGINT        NULL,
  created_by        VARCHAR(50)   NULL,
  created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME      NULL,

  KEY idx_notice_board (published, pinned, notice_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
