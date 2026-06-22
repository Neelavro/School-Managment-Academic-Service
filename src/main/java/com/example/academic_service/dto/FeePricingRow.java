package com.example.academic_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A single class × amount row in the pricing matrix.
 * Used both for reading the current matrix and bulk updating it.
 *
 * Read response semantics:
 *   - For every class in the system, one row is returned.
 *   - amount = null means no rate is set for that class.
 *
 * Bulk update semantics (PUT):
 *   - amount > 0     → upsert that row
 *   - amount == null → delete that row if it exists
 *   - amount == 0    → also delete (treated as "no charge")
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeePricingRow {
    private Integer classId;
    private String className;
    private BigDecimal amount;
}
