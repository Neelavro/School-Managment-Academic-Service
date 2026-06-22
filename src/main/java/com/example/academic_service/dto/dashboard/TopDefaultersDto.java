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
public class TopDefaultersDto {
    private List<Defaulter> defaulters;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Defaulter {
        private Long enrollmentId;
        private String studentSystemId;
        private String studentName;
        private String className;
        private BigDecimal outstandingAmount;
        private int outstandingInvoiceCount;
        private LocalDate oldestUnpaidPeriod;
    }
}
