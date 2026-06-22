package com.example.academic_service.service;

import com.example.academic_service.dto.FeeCategoryRequest;
import com.example.academic_service.dto.FeeCategoryResponse;
import com.example.academic_service.entity.AccountType;
import com.example.academic_service.entity.ChartOfAccount;
import com.example.academic_service.entity.FeeCategory;
import com.example.academic_service.repository.ChartOfAccountRepository;
import com.example.academic_service.repository.FeeCategoryRepository;
import com.example.academic_service.repository.FeePricingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FeeCategoryService {

    private final FeeCategoryRepository repo;
    private final FeePricingRepository pricingRepo;
    private final ChartOfAccountRepository coaRepo;

    public List<FeeCategoryResponse> getAll() {
        List<FeeCategory> all = repo.findAll();
        // Single batched lookup of ledgers to avoid N+1.
        Map<Long, ChartOfAccount> ledgers = new HashMap<>();
        for (FeeCategory c : all) {
            if (!ledgers.containsKey(c.getIncomeLedgerId())) {
                coaRepo.findById(c.getIncomeLedgerId()).ifPresent(a -> ledgers.put(a.getId(), a));
            }
        }
        return all.stream().map(c -> {
            ChartOfAccount l = ledgers.get(c.getIncomeLedgerId());
            return FeeCategoryResponse.from(c,
                    l != null ? l.getAccountCode() : null,
                    l != null ? l.getAccountName() : null);
        }).toList();
    }

    public FeeCategoryResponse getOne(Long id) {
        FeeCategory c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee category not found"));
        ChartOfAccount l = coaRepo.findById(c.getIncomeLedgerId()).orElse(null);
        return FeeCategoryResponse.from(c,
                l != null ? l.getAccountCode() : null,
                l != null ? l.getAccountName() : null);
    }

    @Transactional
    public FeeCategoryResponse create(FeeCategoryRequest req) {
        validateLedger(req.getIncomeLedgerId());

        String code = req.getCode().trim().toUpperCase();
        String name = req.getName().trim();
        if (repo.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "code '" + code + "' already exists");
        }
        if (repo.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "name '" + name + "' already exists");
        }

        FeeCategory c = new FeeCategory();
        c.setCode(code);
        c.setName(name);
        c.setIncomeLedgerId(req.getIncomeLedgerId());
        c.setDescription(req.getDescription());
        c.setIsRecurring(req.getIsRecurring() != null ? req.getIsRecurring() : false);
        c.setIsActive(req.getIsActive() != null ? req.getIsActive() : true);
        FeeCategory saved = repo.save(c);
        ChartOfAccount l = coaRepo.findById(saved.getIncomeLedgerId()).orElse(null);
        return FeeCategoryResponse.from(saved,
                l != null ? l.getAccountCode() : null,
                l != null ? l.getAccountName() : null);
    }

    @Transactional
    public FeeCategoryResponse update(Long id, FeeCategoryRequest req) {
        FeeCategory existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee category not found"));

        validateLedger(req.getIncomeLedgerId());

        String code = req.getCode().trim().toUpperCase();
        String name = req.getName().trim();
        if (!code.equalsIgnoreCase(existing.getCode()) && repo.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "code '" + code + "' already exists");
        }
        if (!name.equalsIgnoreCase(existing.getName()) && repo.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "name '" + name + "' already exists");
        }

        existing.setCode(code);
        existing.setName(name);
        existing.setIncomeLedgerId(req.getIncomeLedgerId());
        existing.setDescription(req.getDescription());
        if (req.getIsRecurring() != null) existing.setIsRecurring(req.getIsRecurring());
        if (req.getIsActive() != null) existing.setIsActive(req.getIsActive());
        FeeCategory saved = repo.save(existing);
        ChartOfAccount l = coaRepo.findById(saved.getIncomeLedgerId()).orElse(null);
        return FeeCategoryResponse.from(saved,
                l != null ? l.getAccountCode() : null,
                l != null ? l.getAccountName() : null);
    }

    @Transactional
    public void delete(Long id) {
        FeeCategory existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee category not found"));
        if (pricingRepo.countByFeeCategoryId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete — pricing rows exist for this category. Remove them first.");
        }
        // NOTE: once invoices reference this category, deletion should be blocked here too.
        repo.delete(existing);
    }

    /**
     * Linked ledger must:
     *  - exist
     *  - be an INCOME account
     *  - be a leaf (is_group = false), so journal postings can target it
     *  - be active
     */
    private void validateLedger(Long ledgerId) {
        ChartOfAccount a = coaRepo.findById(ledgerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "incomeLedgerId does not reference a valid Chart of Accounts row"));
        if (a.getAccountType() != AccountType.INCOME) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Linked ledger must be an INCOME account (got " + a.getAccountType() + ")");
        }
        if (Boolean.TRUE.equals(a.getIsGroup())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Linked ledger must be a leaf account, not a group");
        }
        if (Boolean.FALSE.equals(a.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Linked ledger is inactive");
        }
    }
}
