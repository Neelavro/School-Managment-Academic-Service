package com.example.academic_service.dto;

import com.example.academic_service.entity.Payment;
import com.example.academic_service.entity.PaymentMethod;
import com.example.academic_service.entity.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentResponse {
    private Long id;
    private String tranId;
    private Long invoiceId;
    private String invoiceNumber;
    private String studentName;
    private PaymentMethod method;
    private PaymentStatus status;
    private BigDecimal amount;
    private BigDecimal platformFeeAmount;
    private String gatewayTranId;
    private String gatewayCardType;
    private Long mainJournalEntryId;
    private Long feeJournalEntryId;
    private String payerName;
    private String payerEmail;
    private String payerPhone;
    private String initiatedBy;
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;

    public static PaymentResponse from(Payment p, String invoiceNumber, String studentName) {
        PaymentResponse r = new PaymentResponse();
        r.id = p.getId();
        r.tranId = p.getTranId();
        r.invoiceId = p.getInvoiceId();
        r.invoiceNumber = invoiceNumber;
        r.studentName = studentName;
        r.method = p.getMethod();
        r.status = p.getStatus();
        r.amount = p.getAmount();
        r.platformFeeAmount = p.getPlatformFeeAmount();
        r.gatewayTranId = p.getGatewayTranId();
        r.gatewayCardType = p.getGatewayCardType();
        r.mainJournalEntryId = p.getMainJournalEntryId();
        r.feeJournalEntryId = p.getFeeJournalEntryId();
        r.payerName = p.getPayerName();
        r.payerEmail = p.getPayerEmail();
        r.payerPhone = p.getPayerPhone();
        r.initiatedBy = p.getInitiatedBy();
        r.initiatedAt = p.getInitiatedAt();
        r.completedAt = p.getCompletedAt();
        return r;
    }
}
