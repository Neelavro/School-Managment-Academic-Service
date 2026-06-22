package com.example.academic_service.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CollectionTrendDto {
    private List<MonthPoint> months;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthPoint {
        private LocalDate period;
        /** Pretty display label, e.g. "Jun 2026". */
        private String label;
        private BigDecimal invoiced;
        private BigDecimal collected;
        private BigDecimal outstanding;
    }
}
