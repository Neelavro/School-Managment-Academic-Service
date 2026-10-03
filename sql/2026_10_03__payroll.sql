-- ============================================================================
-- Payroll
-- ============================================================================
-- Accounts sets up salary parts (each posting to an expense account), every
-- designation's standard salary and festival bonus, and the payroll accounts.
-- HR then sets employees' own amounts from a month onward (history kept) and
-- their own bonus (wins over the designation's).
-- A payroll run is a month's salaries or a festival bonus: DRAFT → FINALISED
-- (Dr expense / Cr Salaries Payable) → payslips paid in full, one or many at
-- a time (Dr Salaries Payable / Cr Cash or Bank).
--
-- Idempotent — safe to re-run.
-- ============================================================================

CREATE TABLE IF NOT EXISTS salary_component (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  code              VARCHAR(50)  NOT NULL,
  name              VARCHAR(255) NOT NULL,
  expense_ledger_id BIGINT       NOT NULL,
  is_basic          BIT(1)       NOT NULL DEFAULT b'0',
  sort_order        INT          NOT NULL DEFAULT 0,
  is_active         BIT(1)       NOT NULL DEFAULT b'1',
  created_at        DATETIME(6)      NULL,
  updated_at        DATETIME(6)      NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_salary_component_code (code),
  CONSTRAINT fk_salary_component_ledger FOREIGN KEY (expense_ledger_id) REFERENCES chart_of_accounts(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS designation_salary (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  designation_id INT           NOT NULL,
  component_id   BIGINT        NOT NULL,
  amount         DECIMAL(15,2) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_designation_salary (designation_id, component_id),
  CONSTRAINT fk_designation_salary_designation FOREIGN KEY (designation_id) REFERENCES designation(id) ON DELETE CASCADE,
  CONSTRAINT fk_designation_salary_component   FOREIGN KEY (component_id)   REFERENCES salary_component(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS staff_salary (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  staff_id       BIGINT        NOT NULL,
  component_id   BIGINT        NOT NULL,
  amount         DECIMAL(15,2)     NULL,   -- NULL = follows the designation again from this month
  effective_from DATE          NOT NULL,   -- first day of a month
  created_by     VARCHAR(100)      NULL,
  created_at     DATETIME(6)       NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_staff_salary (staff_id, component_id, effective_from),
  KEY idx_staff_salary_from (effective_from),
  CONSTRAINT fk_staff_salary_staff     FOREIGN KEY (staff_id)     REFERENCES staff(id) ON DELETE CASCADE,
  CONSTRAINT fk_staff_salary_component FOREIGN KEY (component_id) REFERENCES salary_component(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS bonus_rule (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  designation_id INT               NULL,
  staff_id       BIGINT            NULL,
  bonus_type     VARCHAR(20)   NOT NULL,   -- PERCENT_OF_BASIC | FIXED
  bonus_value    DECIMAL(15,2) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_bonus_rule_designation (designation_id),
  UNIQUE KEY uq_bonus_rule_staff (staff_id),
  CONSTRAINT fk_bonus_rule_designation FOREIGN KEY (designation_id) REFERENCES designation(id) ON DELETE CASCADE,
  CONSTRAINT fk_bonus_rule_staff       FOREIGN KEY (staff_id)       REFERENCES staff(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS payroll_settings (
  id                        BIGINT NOT NULL,
  salary_payable_account_id BIGINT NULL,
  bonus_expense_account_id  BIGINT NULL,
  cash_account_id           BIGINT NULL,
  bank_account_id           BIGINT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_payroll_settings_payable FOREIGN KEY (salary_payable_account_id) REFERENCES chart_of_accounts(id) ON DELETE RESTRICT,
  CONSTRAINT fk_payroll_settings_bonus   FOREIGN KEY (bonus_expense_account_id)  REFERENCES chart_of_accounts(id) ON DELETE RESTRICT,
  CONSTRAINT fk_payroll_settings_cash    FOREIGN KEY (cash_account_id)           REFERENCES chart_of_accounts(id) ON DELETE RESTRICT,
  CONSTRAINT fk_payroll_settings_bank    FOREIGN KEY (bank_account_id)           REFERENCES chart_of_accounts(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS payroll_run (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  run_type         VARCHAR(10)  NOT NULL,  -- SALARY | BONUS
  period           DATE         NOT NULL,  -- first day of the month
  title            VARCHAR(255) NOT NULL,
  status           VARCHAR(20)  NOT NULL,  -- DRAFT | FINALISED | CANCELLED
  journal_entry_id BIGINT           NULL,
  created_by       VARCHAR(100)     NULL,
  finalised_by     VARCHAR(100)     NULL,
  finalised_at     DATETIME(6)      NULL,
  created_at       DATETIME(6)      NULL,
  updated_at       DATETIME(6)      NULL,
  PRIMARY KEY (id),
  KEY idx_payroll_run_period (run_type, period),
  CONSTRAINT fk_payroll_run_journal FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS payslip (
  id                       BIGINT        NOT NULL AUTO_INCREMENT,
  payroll_run_id           BIGINT        NOT NULL,
  staff_id                 BIGINT        NOT NULL,
  staff_system_id          VARCHAR(50)       NULL,
  staff_name               VARCHAR(255)  NOT NULL,
  designation_name         VARCHAR(255)      NULL,
  total_amount             DECIMAL(15,2) NOT NULL,
  is_paid                  BIT(1)        NOT NULL DEFAULT b'0',
  paid_on                  DATE              NULL,
  payment_method           VARCHAR(10)       NULL,
  payment_journal_entry_id BIGINT            NULL,
  paid_by                  VARCHAR(100)      NULL,
  created_at               DATETIME(6)       NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_payslip_run_staff (payroll_run_id, staff_id),
  KEY idx_payslip_staff (staff_id),
  CONSTRAINT fk_payslip_run     FOREIGN KEY (payroll_run_id)           REFERENCES payroll_run(id) ON DELETE CASCADE,
  CONSTRAINT fk_payslip_staff   FOREIGN KEY (staff_id)                 REFERENCES staff(id) ON DELETE RESTRICT,
  CONSTRAINT fk_payslip_journal FOREIGN KEY (payment_journal_entry_id) REFERENCES journal_entries(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS payslip_line (
  id                BIGINT        NOT NULL AUTO_INCREMENT,
  payslip_id        BIGINT        NOT NULL,
  component_id      BIGINT            NULL,  -- NULL = festival bonus line
  name              VARCHAR(255)  NOT NULL,
  amount            DECIMAL(15,2) NOT NULL,
  expense_ledger_id BIGINT        NOT NULL,
  sort_order        INT           NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_payslip_line_payslip (payslip_id),
  KEY idx_payslip_line_component (component_id),
  CONSTRAINT fk_payslip_line_payslip FOREIGN KEY (payslip_id)        REFERENCES payslip(id) ON DELETE CASCADE,
  CONSTRAINT fk_payslip_line_ledger  FOREIGN KEY (expense_ledger_id) REFERENCES chart_of_accounts(id) ON DELETE RESTRICT
);
