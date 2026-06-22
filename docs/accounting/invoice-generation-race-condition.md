# Invoice Generation — Concurrency Design Note

**Status:** Accepted limitation in v1. No fix planned until the risk materializes.
**Last updated:** 2026-06-22

---

## The feature

The school admin runs **Generate Invoices for {month}** from the Accounting → Invoices page. The system iterates active enrollments and, for each one:

1. Looks up the applicable Fee Categories and their per-class pricing.
2. Creates an `Invoice` row with sequential number `INV-YYYYMM-NNNNN`.
3. Creates one `InvoiceLine` per applicable fee category (denormalising `feeCategoryName` and `incomeLedgerId`).
4. Posts a balanced accrual `JournalEntry`:
   - **Dr** `Accounts Receivable` (total amount)
   - **Cr** each unique income ledger (summed across all line items)
5. Stamps the resulting `journalEntryId` back onto the invoice.

Each invoice is created in its own `REQUIRES_NEW` transaction so that a failure on one student does not roll back the entire batch. Failures are surfaced in the UI with per-student warnings; re-running Generate is safe — already-invoiced students are skipped via the `findEnrollmentsAlreadyInvoiced` check, which excludes `CANCELLED` invoices so that cancellation followed by regeneration just works.

## The purpose of the design

- **Per-student transactions** → one bad row doesn't kill the whole batch.
- **Cancelled invoices excluded from the "already invoiced" check** → admin can cancel a mistaken invoice and regenerate cleanly without database surgery.
- **No `(enrollment_id, billing_period)` unique constraint** at the DB level — that constraint would block the cancel-then-regenerate flow described above.

The trade-off: removing that unique constraint opens a narrow race-condition window. This document describes the window precisely so future maintainers know what's allowed to happen and what isn't.

---

## The race condition — full scenario

Two admins (Alice and Bob) click **Generate for June 2026** at nearly the same instant. Same student, Aisha, ends up double-billed. Timeline:

```
T=0ms     Alice clicks "Generate for June 2026"
T=0ms     Bob clicks "Generate for June 2026"
          (within the same ms — coordinated click, double-submit, or buggy retry)

T=10ms    Alice's request hits the backend. Thread A starts generate().
          Loads settings, fee categories, candidates (1000 students).
          findEnrollmentsAlreadyInvoiced(June 2026, [1..1000]) → []  (nothing yet)

T=12ms    Bob's request hits the backend in parallel. Thread B starts generate().
          Same query → still []  (Alice hasn't committed anything)

T=20ms    Thread A reaches Aisha.
          alreadyInvoiced.contains(Aisha) → false
          Calls createOneInvoiceTx(Aisha) → opens TRANSACTION A
            - Re-checks findEnrollmentsAlreadyInvoiced → still []
            - Creates Invoice INV-202606-00001 for Aisha
            - Posts journal entry JE-20260622-0001
            (Uncommitted — visible only to TRANSACTION A)

T=21ms    Thread B reaches Aisha.
          alreadyInvoiced.contains(Aisha) → false
          Calls createOneInvoiceTx(Aisha) → opens TRANSACTION B
            - Re-checks findEnrollmentsAlreadyInvoiced → still []
              (REPEATABLE READ isolation hides A's uncommitted writes from B)
            - Creates Invoice INV-202606-00002 for Aisha
            - Posts journal entry JE-20260622-0002
            (Uncommitted)

T=25ms    TRANSACTION A commits. ← Invoice #1 now exists in DB.
T=27ms    TRANSACTION B commits. ← Invoice #2 ALSO now exists. Duplicate!
```

### Necessary conditions for the race

All of these must hold simultaneously:

1. Two admins (or two requests) hit the Generate endpoint within ~30ms of each other.
2. Both requests target the **same period** AND the **same enrollment** is in scope for both.
3. The backend serves them in parallel (default Spring Boot allows 200+ concurrent worker threads, so yes).

### Realistic triggers

- Two admins on a phone call: "Click Generate now" → both click within ms.
- A double-click on the Generate button if the button doesn't disable fast enough.
- A network glitch causing the browser to retry while the original request is still in flight.
- A buggy automation script that calls the endpoint twice.

### When the race CAN'T happen

- Sequential clicks even one second apart — the first batch finishes long before the second starts.
- Two admins working on **different periods** (e.g. June vs July).
- Two admins using **disjoint `classIds` filters** so their candidate sets don't overlap on the same student.
- A single admin clicking once and waiting for the result.

### Blast radius if it does happen

- Aisha ends up with **two invoices for June 2026** (e.g. INV-202606-00001 and INV-202606-00002).
- Two corresponding journal entries: each is correctly balanced on its own.
- Total debits and credits **still balance** across the books — no orphaned half-entries, no negative cash, no broken trial balance.
- The duplicate is visible in the Invoices list. The admin cancels one (which reverses its journal entry via an `ADJUSTMENT` reversal entry — see `JournalEntryService.reverse`).

In other words: the race produces an **annoying duplicate**, not a corrupted ledger.

---

## Why we accept it for v1

- Single school, 2–3 admins, no automation calling the endpoint. Realistic incidence: **near zero in a year of normal use**.
- The fix has real complexity (see options below) that isn't justified by the risk.
- The damage is **detectable** (duplicate invoice numbers in the list) and **reversible** (cancel + reverse JE).
- The accounting equation stays intact regardless.

---

## Mitigation options (deferred, in order of cost)

If the risk ever materializes — or before going multi-tenant / multi-admin-per-school — apply these in order.

### 1. Frontend: disable the Generate button after click (cheapest)

The Generate button stays disabled until the API response returns or fails. Combined with human reaction time this eliminates the double-click and most coordinated-click cases.

**Effort:** trivial. Just a `setState(_busy = true)` already-disable pattern.
**Coverage:** ~90% of realistic cases. Doesn't help against retry or automation.

### 2. Frontend + server: idempotency key

The frontend generates a UUID per Generate click and sends it as `X-Idempotency-Key`. The server stores keys for a short window (e.g. 5 minutes) and returns the cached result for repeats.

**Effort:** moderate — needs a Redis or DB-backed idempotency table.
**Coverage:** retries handled. Multi-admin coordinated clicks not covered (different keys).

### 3. Server: MySQL generated-column unique index (most thorough)

Add a computed column to `invoices` that is `NULL` when `status = CANCELLED` and otherwise a unique key. MySQL allows multiple NULLs in a unique constraint, so cancelled rows don't conflict, but two non-cancelled rows for the same (enrollment, period) do.

```sql
ALTER TABLE invoices
  ADD COLUMN active_period_key VARCHAR(64) GENERATED ALWAYS AS (
    CASE
      WHEN status = 'CANCELLED' THEN NULL
      ELSE CONCAT(enrollment_id, '|', billing_period)
    END
  ) STORED,
  ADD UNIQUE KEY uq_invoice_active_period (active_period_key);
```

The race-losing transaction throws a unique-violation, which we already catch as a per-student failure in `InvoiceService.generate`. The user sees "Failed for Aisha: duplicate invoice for this period" and the other admin's invoice succeeds. No duplicates ever land.

**Effort:** ~15 lines of SQL + acceptance that cancel-then-regenerate still works (it does — the cancelled row's `active_period_key` is NULL).
**Coverage:** 100% — race-proof at the database level.

### 4. Server: serialise generation runs with an advisory lock

`SELECT GET_LOCK('invoice_generation:' + period, …)` at the start of `generate()`. Only one batch per period can run at a time across the whole database. Other concurrent batches wait or fail fast.

**Effort:** moderate — needs lock management and a sensible timeout policy.
**Coverage:** 100%, but adds latency under concurrent load.

## Recommendation

Defer all options for now. Revisit when any of these become true:

- A second school onboards (multi-tenant + automation = real concurrency).
- We add a scheduled monthly auto-generation job (it could overlap with manual clicks).
- A duplicate is ever reported in production.

When that happens, **option 3** (the generated-column unique index) is the right fix. It's bulletproof and the code already handles unique-violation as a per-student failure cleanly.
