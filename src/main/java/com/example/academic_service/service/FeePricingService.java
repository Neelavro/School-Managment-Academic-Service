package com.example.academic_service.service;

import com.example.academic_service.dto.FeePricingRow;
import com.example.academic_service.entity.Class;
import com.example.academic_service.entity.FeePricing;
import com.example.academic_service.repository.ClassRepository;
import com.example.academic_service.repository.FeeCategoryRepository;
import com.example.academic_service.repository.FeePricingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FeePricingService {

    private final FeePricingRepository pricingRepo;
    private final FeeCategoryRepository categoryRepo;
    private final ClassRepository classRepo;

    /**
     * Returns one row per class in the system.
     * If a pricing row exists, amount is set; otherwise amount is null.
     */
    public List<FeePricingRow> getMatrix(Long feeCategoryId) {
        if (!categoryRepo.existsById(feeCategoryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee category not found");
        }
        List<Class> classes = classRepo.findAll();
        List<FeePricing> existing = pricingRepo.findByFeeCategoryId(feeCategoryId);
        Map<Integer, BigDecimal> amountByClass = new HashMap<>();
        for (FeePricing p : existing) amountByClass.put(p.getClassId(), p.getAmount());

        return classes.stream()
                .map(c -> new FeePricingRow(c.getId(), c.getName(), amountByClass.get(c.getId())))
                .sorted((a, b) -> a.getClassName().compareToIgnoreCase(b.getClassName()))
                .toList();
    }

    /**
     * Bulk upsert/delete of the pricing matrix for one fee category.
     * For each row:
     *   amount > 0  → upsert
     *   amount null or <= 0 → delete that row if present
     * Wrapped in a single transaction.
     */
    @Transactional
    public List<FeePricingRow> updateMatrix(Long feeCategoryId, List<FeePricingRow> rows) {
        if (!categoryRepo.existsById(feeCategoryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee category not found");
        }
        if (rows == null) rows = List.of();

        // Pre-validate every classId before any write.
        for (FeePricingRow row : rows) {
            if (row.getClassId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "classId is required");
            }
            if (!classRepo.existsById(row.getClassId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "classId " + row.getClassId() + " does not exist");
            }
        }

        // Load all existing rows once for this category, key by classId.
        Map<Integer, FeePricing> existingByClass = new HashMap<>();
        for (FeePricing p : pricingRepo.findByFeeCategoryId(feeCategoryId)) {
            existingByClass.put(p.getClassId(), p);
        }

        for (FeePricingRow row : rows) {
            BigDecimal amt = row.getAmount();
            FeePricing existing = existingByClass.get(row.getClassId());

            boolean shouldDelete = amt == null || amt.compareTo(BigDecimal.ZERO) <= 0;
            if (shouldDelete) {
                if (existing != null) {
                    pricingRepo.delete(existing);
                    existingByClass.remove(row.getClassId());
                }
                continue;
            }

            if (existing != null) {
                if (existing.getAmount().compareTo(amt) != 0) {
                    existing.setAmount(amt);
                    pricingRepo.save(existing);
                }
            } else {
                FeePricing p = new FeePricing();
                p.setFeeCategoryId(feeCategoryId);
                p.setClassId(row.getClassId());
                p.setAmount(amt);
                p.setEffectiveFrom(LocalDate.now());
                p.setIsActive(true);
                pricingRepo.save(p);
            }
        }

        return getMatrix(feeCategoryId);
    }
}
