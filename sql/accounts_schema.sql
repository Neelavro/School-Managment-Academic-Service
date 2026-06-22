-- ============================================================================
-- academic_service — accounting schema
-- ============================================================================
-- Consolidated CREATE TABLE statements for all accounting + payment + reports
-- work. Replaces the per-phase migrations under sql/migrations/.
--
-- Run order:
--   1. Base academic tables (enrollment, class, student, etc.) MUST already exist
--      — they're created by the application's own schema, not by us.
--   2. Then run this file.
--
--   mysql -u root -p academic_service < sql/schema.sql
--
-- Idempotent: every statement uses CREATE TABLE IF NOT EXISTS, so re-running
-- against an existing DB is harmless. To truly start over:
--
--   DROP TABLE IF EXISTS
--     payment_failures, payments, vouchers,
--     invoice_lines, invoices,
--     journal_entry_lines, journal_entries,
--     accounting_settings,
--     fee_pricing, fee_categories,
--     chart_of_accounts;
-- ============================================================================

-- ── chart_of_accounts ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS chart_of_accounts (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  account_code  VARCHAR(50)  NOT NULL,
  account_name  VARCHAR(255) NOT NULL,
  account_type  VARCHAR(20)  NOT NULL,            -- ASSET | LIABILITY | INCOME | EXPENSE
  parent_id     BIGINT           NULL,
  is_group      BIT(1)       NOT NULL DEFAULT b'0',
  is_active     BIT(1)       NOT NULL DEFAULT b'1',
  description   TEXT             NULL,
  created_at    DATETIME(6)      NULL,
  updated_at    DATETIME(6)      NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_coa_code (account_code),
  KEY idx_coa_parent_id (parent_id),
  KEY idx_coa_account_type (account_type),
  KEY idx_coa_is_active (is_active),
  CONSTRAINT fk_coa_parent FOREIGN KEY (parent_id)
      REFERENCES chart_of_accounts(id) ON DELETE RESTRICT
);

-- ── fee_categories ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS fee_categories (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  code              VARCHAR(50)  NOT NULL,
  name              VARCHAR(255) NOT NULL,
  income_ledger_id  BIGINT       NOT NULL,
  description       TEXT             NULL,
  is_recurring      BIT(1)       NOT NULL DEFAULT b'0',
  is_active         BIT(1)       NOT NULL DEFAULT b'1',
  created_at        DATETIME(6)      NULL,
  updated_at        DATETIME(6)      NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_fee_category_code (code),
  UNIQUE KEY uq_fee_category_name (name),
  KEY idx_fee_category_ledger (income_ledger_id),
  KEY idx_fee_category_active (is_active),
  CONSTRAINT fk_fee_category_ledger FOREIGN KEY (income_ledger_id)
      REFERENCES chart_of_accounts(id) ON DELETE RESTRICT
);

-- ── fee_pricing ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS fee_pricing (
  id                BIGINT          NOT NULL AUTO_INCREMENT,
  fee_category_id   BIGINT          NOT NULL,
  class_id          BIGINT          NOT NULL,
  amount            DECIMAL(15,2)   NOT NULL,
  effective_from    DATE                NULL,
  is_active         BIT(1)          NOT NULL DEFAULT b'1',
  created_at        DATETIME(6)         NULL,
  updated_at        DATETIME(6)         NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_fee_pricing_cat_class (fee_category_id, class_id),
  KEY idx_fee_pricing_category (fee_category_id),
  KEY idx_fee_pricing_class (class_id),
  CONSTRAINT fk_fee_pricing_category FOREIGN KEY (fee_category_id)
      REFERENCES fee_categories(id) ON DELETE CASCADE,
  CONSTRAINT fk_fee_pricing_class FOREIGN KEY (class_id)
      REFERENCES class(id) ON DELETE RESTRICT
);

-- ── accounting_settings (singleton row id=1) ──────────────────────────────
CREATE TABLE IF NOT EXISTS accounting_settings (
  id                            BIGINT          NOT NULL,
  ar_account_id                 BIGINT              NULL,
  cash_account_id               BIGINT              NULL,
  gateway_clearing_account_id   BIGINT              NULL,
  platform_fee_account_id       BIGINT              NULL,
  platform_payable_account_id   BIGINT              NULL,
  platform_fee_percent          DECIMAL(5,2)        NULL,
  platform_fee_flat             DECIMAL(15,2)       NULL,
  invoice_due_days              INT             NOT NULL DEFAULT 7,
  invoice_number_prefix         VARCHAR(16)     NOT NULL DEFAULT 'INV',
  journal_number_prefix         VARCHAR(16)     NOT NULL DEFAULT 'JE',
  created_at                    DATETIME(6)         NULL,
  updated_at                    DATETIME(6)         NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_settings_ar
      FOREIGN KEY (ar_account_id)               REFERENCES chart_of_accounts(id) ON DELETE RESTRICT,
  CONSTRAINT fk_settings_cash
      FOREIGN KEY (cash_account_id)             REFERENCES chart_of_accounts(id) ON DELETE RESTRICT,
  CONSTRAINT fk_settings_gateway_clearing
      FOREIGN KEY (gateway_clearing_account_id) REFERENCES chart_of_accounts(id) ON DELETE RESTRICT,
  CONSTRAINT fk_settings_platform_fee
      FOREIGN KEY (platform_fee_account_id)     REFERENCES chart_of_accounts(id) ON DELETE RESTRICT,
  CONSTRAINT fk_settings_platform_payable
      FOREIGN KEY (platform_payable_account_id) REFERENCES chart_of_accounts(id) ON DELETE RESTRICT
);

-- The settings row must exist before invoices can post.
INSERT IGNORE INTO accounting_settings
  (id, invoice_due_days, invoice_number_prefix, journal_number_prefix, created_at, updated_at)
VALUES
  (1, 7, 'INV', 'JE', NOW(6), NOW(6));

-- ── journal_entries ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS journal_entries (
  id                       BIGINT          NOT NULL AUTO_INCREMENT,
  entry_number             VARCHAR(64)     NOT NULL,
  entry_date               DATE            NOT NULL,
  description              TEXT                NULL,
  reference_type           VARCHAR(30)     NOT NULL,
  reference_id             BIGINT              NULL,
  total_debit              DECIMAL(15,2)   NOT NULL,
  total_credit             DECIMAL(15,2)   NOT NULL,
  is_reversed              BIT(1)          NOT NULL DEFAULT b'0',
  reverses_entry_id        BIGINT              NULL,
  reversed_by_entry_id     BIGINT              NULL,
  created_by               VARCHAR(100)        NULL,
  created_at               DATETIME(6)     NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_journal_entry_number (entry_number),
  KEY idx_journal_entry_date (entry_date),
  KEY idx_journal_reference (reference_type, reference_id),
  KEY idx_journal_reversed (is_reversed),
  CONSTRAINT fk_journal_reverses
      FOREIGN KEY (reverses_entry_id)    REFERENCES journal_entries(id) ON DELETE RESTRICT,
  CONSTRAINT fk_journal_reversed_by
      FOREIGN KEY (reversed_by_entry_id) REFERENCES journal_entries(id) ON DELETE RESTRICT
);

-- ── journal_entry_lines ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS journal_entry_lines (
  id                  BIGINT          NOT NULL AUTO_INCREMENT,
  journal_entry_id    BIGINT          NOT NULL,
  account_id          BIGINT          NOT NULL,
  debit_amount        DECIMAL(15,2)       NULL,
  credit_amount       DECIMAL(15,2)       NULL,
  line_description    VARCHAR(500)        NULL,
  PRIMARY KEY (id),
  KEY idx_jel_entry (journal_entry_id),
  KEY idx_jel_account (account_id),
  CONSTRAINT fk_jel_entry   FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id)  ON DELETE CASCADE,
  CONSTRAINT fk_jel_account FOREIGN KEY (account_id)       REFERENCES chart_of_accounts(id) ON DELETE RESTRICT
);

-- ── invoices ──────────────────────────────────────────────────────────────
-- (No (enrollment_id, billing_period) unique here — cancelled invoices need
-- to coexist with regenerations. App logic excludes CANCELLED from the
-- "already invoiced" check; see docs/accounting/invoice-generation-race-condition.md)
CREATE TABLE IF NOT EXISTS invoices (
  id                BIGINT          NOT NULL AUTO_INCREMENT,
  invoice_number    VARCHAR(64)     NOT NULL,
  enrollment_id     BIGINT          NOT NULL,
  billing_period    DATE            NOT NULL,
  issued_date       DATE            NOT NULL,
  due_date          DATE            NOT NULL,
  total_amount      DECIMAL(15,2)   NOT NULL,
  paid_amount       DECIMAL(15,2)   NOT NULL DEFAULT 0,
  status            VARCHAR(20)     NOT NULL,
  notes             TEXT                NULL,
  journal_entry_id  BIGINT              NULL,
  created_at        DATETIME(6)     NOT NULL,
  updated_at        DATETIME(6)     NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_invoice_number (invoice_number),
  KEY idx_invoice_enrollment (enrollment_id),
  KEY idx_invoice_period (billing_period),
  KEY idx_invoice_status (status),
  KEY idx_invoice_due_date (due_date),
  KEY idx_invoice_enr_period_status (enrollment_id, billing_period, status),
  CONSTRAINT fk_invoice_enrollment
      FOREIGN KEY (enrollment_id)    REFERENCES enrollment(id)      ON DELETE RESTRICT,
  CONSTRAINT fk_invoice_journal
      FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id) ON DELETE RESTRICT
);

-- ── invoice_lines ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS invoice_lines (
  id                  BIGINT          NOT NULL AUTO_INCREMENT,
  invoice_id          BIGINT          NOT NULL,
  fee_category_id     BIGINT          NOT NULL,
  fee_category_name   VARCHAR(255)    NOT NULL,
  income_ledger_id    BIGINT          NOT NULL,
  amount              DECIMAL(15,2)   NOT NULL,
  PRIMARY KEY (id),
  KEY idx_invoice_line_invoice (invoice_id),
  KEY idx_invoice_line_category (fee_category_id),
  CONSTRAINT fk_invoice_line_invoice
      FOREIGN KEY (invoice_id)       REFERENCES invoices(id)         ON DELETE CASCADE,
  CONSTRAINT fk_invoice_line_category
      FOREIGN KEY (fee_category_id)  REFERENCES fee_categories(id)   ON DELETE RESTRICT,
  CONSTRAINT fk_invoice_line_ledger
      FOREIGN KEY (income_ledger_id) REFERENCES chart_of_accounts(id) ON DELETE RESTRICT
);

-- ── vouchers ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS vouchers (
  id                       BIGINT          NOT NULL AUTO_INCREMENT,
  voucher_number           VARCHAR(64)     NOT NULL,
  voucher_type             VARCHAR(20)     NOT NULL,    -- RECEIPT | PAYMENT | CONTRA | JOURNAL
  voucher_date             DATE            NOT NULL,
  narration                TEXT                NULL,
  total_amount             DECIMAL(15,2)   NOT NULL,
  party_name               VARCHAR(255)        NULL,
  journal_entry_id         BIGINT          NOT NULL,
  is_reversed              BIT(1)          NOT NULL DEFAULT b'0',
  reverses_voucher_id      BIGINT              NULL,
  reversed_by_voucher_id   BIGINT              NULL,
  created_by               VARCHAR(100)        NULL,
  created_at               DATETIME(6)     NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_voucher_number (voucher_number),
  KEY idx_voucher_date (voucher_date),
  KEY idx_voucher_type (voucher_type),
  KEY idx_voucher_journal (journal_entry_id),
  KEY idx_voucher_reversed (is_reversed),
  CONSTRAINT fk_voucher_journal
      FOREIGN KEY (journal_entry_id)       REFERENCES journal_entries(id) ON DELETE RESTRICT,
  CONSTRAINT fk_voucher_reverses
      FOREIGN KEY (reverses_voucher_id)    REFERENCES vouchers(id)        ON DELETE RESTRICT,
  CONSTRAINT fk_voucher_reversed_by
      FOREIGN KEY (reversed_by_voucher_id) REFERENCES vouchers(id)        ON DELETE RESTRICT
);

-- ── payments (INITIATED + SUCCESS only) ───────────────────────────────────
CREATE TABLE IF NOT EXISTS payments (
  id                       BIGINT          NOT NULL AUTO_INCREMENT,
  tran_id                  VARCHAR(64)     NOT NULL,
  invoice_id               BIGINT          NOT NULL,
  method                   VARCHAR(30)     NOT NULL,   -- ONLINE_SSLCOMMERZ | BANK | CASH
  status                   VARCHAR(20)     NOT NULL,   -- INITIATED | SUCCESS
  amount                   DECIMAL(15,2)   NOT NULL,
  platform_fee_amount      DECIMAL(15,2)       NULL,
  gateway_tran_id          VARCHAR(128)        NULL,
  gateway_card_type        VARCHAR(64)         NULL,
  gateway_response         LONGTEXT            NULL,
  main_journal_entry_id    BIGINT              NULL,
  fee_journal_entry_id     BIGINT              NULL,
  payer_name               VARCHAR(255)        NULL,
  payer_email              VARCHAR(255)        NULL,
  payer_phone              VARCHAR(50)         NULL,
  initiated_by             VARCHAR(100)        NULL,
  initiated_ip             VARCHAR(64)         NULL,
  initiated_at             DATETIME(6)     NOT NULL,
  completed_at             DATETIME(6)         NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_payment_tran_id (tran_id),
  KEY idx_payment_invoice (invoice_id),
  KEY idx_payment_status (status),
  KEY idx_payment_method (method),
  KEY idx_payment_initiated_at (initiated_at),
  CONSTRAINT fk_payment_invoice
      FOREIGN KEY (invoice_id)            REFERENCES invoices(id)        ON DELETE RESTRICT,
  CONSTRAINT fk_payment_main_journal
      FOREIGN KEY (main_journal_entry_id) REFERENCES journal_entries(id) ON DELETE RESTRICT,
  CONSTRAINT fk_payment_fee_journal
      FOREIGN KEY (fee_journal_entry_id)  REFERENCES journal_entries(id) ON DELETE RESTRICT
);

-- ── payment_failures (FAILED + CANCELLED archive) ─────────────────────────
CREATE TABLE IF NOT EXISTS payment_failures (
  id                BIGINT          NOT NULL AUTO_INCREMENT,
  tran_id           VARCHAR(64)     NOT NULL,
  invoice_id        BIGINT          NOT NULL,
  method            VARCHAR(30)     NOT NULL,
  status            VARCHAR(20)     NOT NULL,            -- FAILED | CANCELLED
  amount            DECIMAL(15,2)   NOT NULL,
  gateway_tran_id   VARCHAR(128)        NULL,
  gateway_card_type VARCHAR(64)         NULL,
  gateway_response  LONGTEXT            NULL,
  failure_reason    VARCHAR(500)        NULL,
  payer_name        VARCHAR(255)        NULL,
  payer_email       VARCHAR(255)        NULL,
  payer_phone       VARCHAR(50)         NULL,
  initiated_by      VARCHAR(100)        NULL,
  initiated_ip      VARCHAR(64)         NULL,
  initiated_at      DATETIME(6)     NOT NULL,
  failed_at         DATETIME(6)     NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_payment_failure_tran_id (tran_id),
  KEY idx_payment_failure_invoice (invoice_id),
  KEY idx_payment_failure_status (status),
  KEY idx_payment_failure_failed_at (failed_at),
  CONSTRAINT fk_payment_failure_invoice
      FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE RESTRICT
);


