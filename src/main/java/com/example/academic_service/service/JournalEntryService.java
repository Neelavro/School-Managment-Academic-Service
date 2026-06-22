package com.example.academic_service.service;

import com.example.academic_service.dto.JournalEntryRequest;
import com.example.academic_service.dto.JournalEntryResponse;
import com.example.academic_service.dto.JournalLineRequest;
import com.example.academic_service.dto.JournalLineResponse;
import com.example.academic_service.entity.*;
import com.example.academic_service.repository.AccountingSettingsRepository;
import com.example.academic_service.repository.ChartOfAccountRepository;
import com.example.academic_service.repository.JournalEntryLineRepository;
import com.example.academic_service.repository.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Core engine for posting balanced double-entry journal entries.
 *
 * Invariants (enforced here):
 *   - Sum of debits must equal sum of credits (to 2dp).
 *   - Every line must reference an existing, leaf, active CoA account.
 *   - Each line is either debit or credit, never both, never zero or negative.
 *   - Once posted, an entry's lines and amounts cannot be modified.
 *     Corrections are made by posting a reversal entry.
 */
@Service
@RequiredArgsConstructor
public class JournalEntryService {

    private final JournalEntryRepository entryRepo;
    private final JournalEntryLineRepository lineRepo;
    private final ChartOfAccountRepository coaRepo;
    private final AccountingSettingsRepository settingsRepo;

    private static final DateTimeFormatter DATE_TAG = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ── Read ──────────────────────────────────────────────────────────────

    public JournalEntryResponse getOne(Long id) {
        JournalEntry e = entryRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journal entry not found"));
        return hydrate(e);
    }

    public Page<JournalEntryResponse> search(JournalReferenceType type,
                                              LocalDate from, LocalDate to,
                                              int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));
        Page<JournalEntry> entries = entryRepo.search(type, from, to, pageable);
        if (entries.isEmpty()) return entries.map(this::hydrate);
        // Batch-load lines and account codes/names to avoid N+1.
        List<Long> ids = entries.stream().map(JournalEntry::getId).toList();
        List<JournalEntryLine> allLines = lineRepo.findByJournalEntryIdIn(ids);
        Map<Long, List<JournalEntryLine>> linesByEntry = new HashMap<>();
        Set<Long> accountIds = new HashSet<>();
        for (JournalEntryLine l : allLines) {
            linesByEntry.computeIfAbsent(l.getJournalEntryId(), k -> new ArrayList<>()).add(l);
            accountIds.add(l.getAccountId());
        }
        Map<Long, ChartOfAccount> accounts = new HashMap<>();
        for (ChartOfAccount a : coaRepo.findAllById(accountIds)) accounts.put(a.getId(), a);
        return entries.map(e -> hydrateWith(e, linesByEntry.getOrDefault(e.getId(), List.of()), accounts));
    }

    public List<JournalEntryResponse> findByReference(JournalReferenceType type, Long referenceId) {
        return entryRepo.findByReferenceTypeAndReferenceId(type, referenceId).stream()
                .map(this::hydrate)
                .toList();
    }

    // ── Post ──────────────────────────────────────────────────────────────

    /**
     * Posts a new balanced journal entry. Validates Dr = Cr, looks up every account,
     * computes totals, generates an entry_number, and saves header + lines in one tx.
     */
    @Transactional
    public JournalEntryResponse post(JournalEntryRequest req, String createdBy) {
        return postInternal(req, createdBy, null);
    }

    /**
     * Internal variant used by reverse() to set reverses_entry_id atomically.
     */
    @Transactional
    protected JournalEntryResponse postInternal(JournalEntryRequest req, String createdBy, Long reversesId) {
        validateRequest(req);
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        // Pre-load all accounts referenced by lines. Single batched query.
        Set<Long> accountIds = new HashSet<>();
        for (JournalLineRequest l : req.getLines()) accountIds.add(l.getAccountId());
        Map<Long, ChartOfAccount> accounts = new HashMap<>();
        for (ChartOfAccount a : coaRepo.findAllById(accountIds)) accounts.put(a.getId(), a);

        for (JournalLineRequest line : req.getLines()) {
            ChartOfAccount account = accounts.get(line.getAccountId());
            if (account == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "accountId " + line.getAccountId() + " does not exist");
            }
            if (Boolean.TRUE.equals(account.getIsGroup())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot post to group account " + account.getAccountCode());
            }
            if (Boolean.FALSE.equals(account.getIsActive())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Account " + account.getAccountCode() + " is inactive");
            }

            BigDecimal d = line.getDebit();
            BigDecimal c = line.getCredit();
            boolean hasDebit = d != null && d.compareTo(BigDecimal.ZERO) > 0;
            boolean hasCredit = c != null && c.compareTo(BigDecimal.ZERO) > 0;
            if (hasDebit == hasCredit) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each line must have exactly one of debit or credit > 0 (account " + account.getAccountCode() + ")");
            }
            if (hasDebit) totalDebit = totalDebit.add(d);
            else totalCredit = totalCredit.add(c);
        }

        // Balance check at 2dp precision.
        BigDecimal dr = totalDebit.setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal cr = totalCredit.setScale(2, java.math.RoundingMode.HALF_UP);
        if (dr.compareTo(cr) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debits (" + dr + ") and credits (" + cr + ") do not balance");
        }

        // Create the header.
        JournalEntry header = new JournalEntry();
        header.setEntryNumber(nextEntryNumber(req.getEntryDate()));
        header.setEntryDate(req.getEntryDate());
        header.setDescription(req.getDescription());
        header.setReferenceType(req.getReferenceType());
        header.setReferenceId(req.getReferenceId());
        header.setTotalDebit(dr);
        header.setTotalCredit(cr);
        header.setIsReversed(false);
        header.setReversesEntryId(reversesId);
        header.setCreatedBy(createdBy);
        JournalEntry savedHeader = entryRepo.save(header);

        // Save lines.
        List<JournalEntryLine> savedLines = new ArrayList<>();
        for (JournalLineRequest l : req.getLines()) {
            JournalEntryLine line = new JournalEntryLine();
            line.setJournalEntryId(savedHeader.getId());
            line.setAccountId(l.getAccountId());
            // Already validated: exactly one of debit/credit > 0.
            if (l.getDebit() != null && l.getDebit().compareTo(BigDecimal.ZERO) > 0) {
                line.setDebitAmount(l.getDebit().setScale(2, java.math.RoundingMode.HALF_UP));
                line.setCreditAmount(null);
            } else {
                line.setDebitAmount(null);
                line.setCreditAmount(l.getCredit().setScale(2, java.math.RoundingMode.HALF_UP));
            }
            line.setLineDescription(l.getDescription());
            savedLines.add(lineRepo.save(line));
        }

        return hydrateWith(savedHeader, savedLines, accounts);
    }

    // ── Reverse ───────────────────────────────────────────────────────────

    /**
     * Posts a reversing entry that mirrors the original — debits become credits
     * and vice versa. Marks the original as reversed and stores the bidirectional link.
     * The original entry's lines and amounts are NOT changed.
     */
    @Transactional
    public JournalEntryResponse reverse(Long originalId, String reason, String createdBy) {
        JournalEntry original = entryRepo.findById(originalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journal entry not found"));
        if (Boolean.TRUE.equals(original.getIsReversed())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Entry " + original.getEntryNumber() + " has already been reversed");
        }
        if (original.getReversesEntryId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot reverse a reversal entry");
        }

        List<JournalEntryLine> originalLines = lineRepo.findByJournalEntryId(originalId);
        if (originalLines.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Original entry has no lines");
        }

        // Build the mirror request.
        JournalEntryRequest mirror = new JournalEntryRequest();
        mirror.setEntryDate(LocalDate.now());
        mirror.setDescription("Reversal of " + original.getEntryNumber()
                + (reason != null && !reason.isBlank() ? " — " + reason : ""));
        mirror.setReferenceType(JournalReferenceType.ADJUSTMENT);
        mirror.setReferenceId(original.getId());
        List<JournalLineRequest> mirrorLines = new ArrayList<>();
        for (JournalEntryLine l : originalLines) {
            JournalLineRequest m = new JournalLineRequest();
            m.setAccountId(l.getAccountId());
            // Flip debit ↔ credit.
            m.setDebit(l.getCreditAmount());
            m.setCredit(l.getDebitAmount());
            m.setDescription("Reversal: " + (l.getLineDescription() != null ? l.getLineDescription() : ""));
            mirrorLines.add(m);
        }
        mirror.setLines(mirrorLines);

        JournalEntryResponse reversal = postInternal(mirror, createdBy, original.getId());

        // Mark original as reversed.
        original.setIsReversed(true);
        original.setReversedByEntryId(reversal.getId());
        entryRepo.save(original);

        return reversal;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void validateRequest(JournalEntryRequest req) {
        if (req.getEntryDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "entryDate is required");
        }
        if (req.getReferenceType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "referenceType is required");
        }
        if (req.getLines() == null || req.getLines().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At least two lines are required (one Dr, one Cr)");
        }
    }

    /**
     * Generates next sequential entry number: PREFIX-YYYYMMDD-NNNN.
     * Reads the highest existing for today's date and increments.
     * NOTE: relies on the unique constraint on entry_number to detect concurrent
     * inserts — if two transactions race and pick the same number, one will fail
     * with a unique-constraint violation and the caller should retry.
     */
    private String nextEntryNumber(LocalDate date) {
        String prefix = settingsRepo.findById(1L)
                .map(AccountingSettings::getJournalNumberPrefix)
                .filter(p -> p != null && !p.isBlank())
                .orElse("JE");
        String dateTag = date.format(DATE_TAG);
        String search = prefix + "-" + dateTag + "-";
        // Look at top 1.
        List<String> recent = entryRepo.findRecentNumbersByPrefix(search, PageRequest.of(0, 1));
        int next = 1;
        if (!recent.isEmpty()) {
            String last = recent.get(0);
            int dash = last.lastIndexOf('-');
            if (dash > 0 && dash < last.length() - 1) {
                try {
                    next = Integer.parseInt(last.substring(dash + 1)) + 1;
                } catch (NumberFormatException ignored) {
                    // fall through with next = 1
                }
            }
        }
        return String.format("%s%04d", search, next);
    }

    private JournalEntryResponse hydrate(JournalEntry e) {
        List<JournalEntryLine> lines = lineRepo.findByJournalEntryId(e.getId());
        Set<Long> accountIds = new HashSet<>();
        for (JournalEntryLine l : lines) accountIds.add(l.getAccountId());
        Map<Long, ChartOfAccount> accounts = new HashMap<>();
        for (ChartOfAccount a : coaRepo.findAllById(accountIds)) accounts.put(a.getId(), a);
        return hydrateWith(e, lines, accounts);
    }

    private JournalEntryResponse hydrateWith(JournalEntry e,
                                              List<JournalEntryLine> lines,
                                              Map<Long, ChartOfAccount> accounts) {
        List<JournalLineResponse> lineDtos = new ArrayList<>();
        for (JournalEntryLine l : lines) {
            ChartOfAccount a = accounts.get(l.getAccountId());
            lineDtos.add(JournalLineResponse.from(l,
                    a != null ? a.getAccountCode() : null,
                    a != null ? a.getAccountName() : null));
        }
        return JournalEntryResponse.from(e, lineDtos);
    }
}
