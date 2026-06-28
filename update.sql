-- ============================================================================
-- Update SQL — academic_service
-- Apply on existing DBs.
-- ============================================================================

-- ── invoice_lines ──────────────────────────────────────────────────────────
-- fee_category_id must allow NULL so system-injected lines (Platform Fee)
-- that have no fee_category can be persisted.
ALTER TABLE invoice_lines
  MODIFY COLUMN fee_category_id BIGINT NULL;
