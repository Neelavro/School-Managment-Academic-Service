package com.example.academic_service.repository;

import com.example.academic_service.entity.JournalEntry;
import com.example.academic_service.entity.JournalReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    Optional<JournalEntry> findByEntryNumber(String entryNumber);

    List<JournalEntry> findByReferenceTypeAndReferenceId(JournalReferenceType type, Long refId);

    /**
     * Paginated lookup with optional filters. Null parameters mean "no constraint".
     */
    @Query("""
        SELECT j FROM JournalEntry j
        WHERE (:type IS NULL OR j.referenceType = :type)
          AND (:from IS NULL OR j.entryDate >= :from)
          AND (:to   IS NULL OR j.entryDate <= :to)
        ORDER BY j.entryDate DESC, j.id DESC
    """)
    Page<JournalEntry> search(
            @Param("type") JournalReferenceType type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    /** Returns the highest sequence we've assigned for the given prefix + date. */
    @Query("""
        SELECT j.entryNumber FROM JournalEntry j
        WHERE j.entryNumber LIKE CONCAT(:prefix, '%')
        ORDER BY j.id DESC
    """)
    List<String> findRecentNumbersByPrefix(@Param("prefix") String prefix, Pageable pageable);
}
