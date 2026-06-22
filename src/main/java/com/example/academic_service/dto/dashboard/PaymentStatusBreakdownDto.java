package com.example.academic_service.dto.dashboard;

import com.example.academic_service.entity.InvoiceStatus;
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
public class PaymentStatusBreakdownDto {
    private LocalDate period;
    private List<Segment> segments;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Segment {
        private InvoiceStatus status;
        private long count;
        private BigDecimal amount;
        /** 0–100, two-decimal. */
        private BigDecimal percentage;
    }
}
