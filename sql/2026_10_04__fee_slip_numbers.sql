-- ============================================================================
-- Monthly fee slip numbers: FEE-… instead of INV-…
-- ============================================================================
-- People never see "invoice" any more, so the slip numbers stop starting with
-- INV. The default prefix becomes FEE, and existing numbers, payment references
-- and journal text are renamed to match. Online payments still waiting for the
-- gateway (INITIATED) keep their reference: the gateway calls back with it.
-- A school that set its own prefix (not INV) is left alone.
--
-- Idempotent — safe to re-run.
-- ============================================================================

ALTER TABLE accounting_settings ALTER COLUMN invoice_number_prefix SET DEFAULT 'FEE';
UPDATE accounting_settings SET invoice_number_prefix = 'FEE' WHERE invoice_number_prefix = 'INV';

UPDATE invoices SET invoice_number = CONCAT('FEE-', SUBSTRING(invoice_number, 5))
WHERE invoice_number LIKE 'INV-%';

UPDATE payments SET tran_id = REPLACE(tran_id, '-INV-', '-FEE-')
WHERE tran_id LIKE '%-INV-%' AND status <> 'INITIATED';
UPDATE payment_failures SET tran_id = REPLACE(tran_id, '-INV-', '-FEE-')
WHERE tran_id LIKE '%-INV-%';

UPDATE journal_entries
SET description = REPLACE(REPLACE(description, 'Invoice INV-', 'Monthly fee FEE-'), 'INV-', 'FEE-')
WHERE description LIKE '%INV-%';
UPDATE journal_entry_lines SET line_description = REPLACE(line_description, 'INV-', 'FEE-')
WHERE line_description LIKE '%INV-%';
UPDATE invoices SET notes = REPLACE(notes, 'INV-', 'FEE-') WHERE notes LIKE '%INV-%';

-- Check: nothing should still start with INV-.
SELECT COUNT(*) AS slips_still_inv FROM invoices WHERE invoice_number LIKE 'INV-%';
