-- ============================================================================
-- Migration: nullable fee_category_id on invoice_lines (Platform Fee flow)
-- ============================================================================
-- System-injected invoice lines (e.g. the per-invoice Platform Fee line that
-- credits the Payable-to-Platform account) do not belong to any fee_category.
-- This relaxes the FK column so those lines can be persisted.
-- ============================================================================

ALTER TABLE invoice_lines
  MODIFY COLUMN fee_category_id BIGINT NULL;
