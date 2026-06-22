package com.example.academic_service.dto.dashboard;

import com.example.academic_service.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecentPaymentsDto {
    private List<RecentPayment> payments;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentPayment {
        private Long paymentId;
        private String tranId;
        private String studentName;
        private String invoiceNumber;
        private BigDecimal amount;
        private PaymentMethod method;
        private LocalDateTime completedAt;
    }
}
