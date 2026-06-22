package com.example.academic_service.repository;

import com.example.academic_service.entity.JournalEntryLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalEntryLineRepository extends JpaRepository<JournalEntryLine, Long> {
    List<JournalEntryLine> findByJournalEntryId(Long journalEntryId);
    List<JournalEntryLine> findByJournalEntryIdIn(List<Long> entryIds);
    long countByAccountId(Long accountId);

    /**
     * Per-account total debits and credits over a date range. Returns
     * Object[] { Long accountId, BigDecimal totalDebit, BigDecimal totalCredit }.
     *
     * NULL bounds = unbounded on that side. Includes reversed entries —
     * the reversing entry is a separate row that nets the original to zero.
     */
    @org.springframework.data.jpa.repository.Query("""
        SELECT jel.accountId,
               COALESCE(SUM(jel.debitAmount), 0),
               COALESCE(SUM(jel.creditAmount), 0)
          FROM JournalEntryLine jel
          JOIN JournalEntry je ON je.id = jel.journalEntryId
         WHERE (:from IS NULL OR je.entryDate >= :from)
           AND (:to   IS NULL OR je.entryDate <= :to)
         GROUP BY jel.accountId
    """)
    List<Object[]> aggregateByAccount(
            @org.springframework.data.repository.query.Param("from") java.time.LocalDate from,
            @org.springframework.data.repository.query.Param("to") java.time.LocalDate to);

    /**
     * Detail rows for ONE account within a date range, ordered by entry date.
     * Used by the Ledger Report.
     */
    @org.springframework.data.jpa.repository.Query("""
        SELECT jel, je
          FROM JournalEntryLine jel
          JOIN JournalEntry je ON je.id = jel.journalEntryId
         WHERE jel.accountId = :accountId
           AND (:from IS NULL OR je.entryDate >= :from)
           AND (:to   IS NULL OR je.entryDate <= :to)
         ORDER BY je.entryDate ASC, je.id ASC, jel.id ASC
    """)
    List<Object[]> findLedgerLines(
            @org.springframework.data.repository.query.Param("accountId") Long accountId,
            @org.springframework.data.repository.query.Param("from") java.time.LocalDate from,
            @org.springframework.data.repository.query.Param("to") java.time.LocalDate to);
}
