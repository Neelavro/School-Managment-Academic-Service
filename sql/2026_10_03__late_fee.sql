-- ============================================================================
-- Late fee
-- ============================================================================
-- One fee category can be the late fee (is_late_fee), priced per class like
-- any other fee. Once a monthly fee is past its due date and not fully paid,
-- the class's late fee is added to it once: an invoice line, plus its own
-- accrual entry (late_fee_journal_entry_id) that is reversed if the monthly
-- fee is cancelled.
--
-- Idempotent — safe to re-run.
-- ============================================================================

DROP PROCEDURE IF EXISTS late_fee_columns;
DELIMITER //
CREATE PROCEDURE late_fee_columns()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fee_categories' AND COLUMN_NAME = 'is_late_fee') THEN
    ALTER TABLE fee_categories
      ADD COLUMN is_late_fee BIT(1) NOT NULL DEFAULT b'0' AFTER is_recurring;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoices' AND COLUMN_NAME = 'late_fee_amount') THEN
    ALTER TABLE invoices
      ADD COLUMN late_fee_amount           DECIMAL(15,2) NULL AFTER paid_amount,
      ADD COLUMN late_fee_applied_at       DATETIME(6)   NULL AFTER late_fee_amount,
      ADD COLUMN late_fee_journal_entry_id BIGINT        NULL AFTER journal_entry_id,
      ADD CONSTRAINT fk_invoice_late_fee_journal
          FOREIGN KEY (late_fee_journal_entry_id) REFERENCES journal_entries(id) ON DELETE RESTRICT;
  END IF;
END //
DELIMITER ;
CALL late_fee_columns();
DROP PROCEDURE late_fee_columns;
