package com.example.academic_service.dto;

import com.example.academic_service.entity.PaymentFailure;
import com.example.academic_service.entity.PaymentMethod;
import com.example.academic_service.entity.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentFailureResponse {
    private Long id;
    private String tranId;
    private Long invoiceId;
    private String invoiceNumber;
    private String studentName;
    private PaymentMethod method;
    private PaymentStatus status;
    private BigDecimal amount;
    private String gatewayTranId;
    private String gatewayCardType;
    private String failureReason;
    private String payerName;
    private String payerEmail;
    private String payerPhone;
    private String initiatedBy;
    private LocalDateTime initiatedAt;
    private LocalDateTime failedAt;

    public static PaymentFailureResponse from(PaymentFailure f, String invoiceNumber, String studentName) {
        PaymentFailureResponse r = new PaymentFailureResponse();
        r.id = f.getId();
        r.tranId = f.getTranId();
        r.invoiceId = f.getInvoiceId();
        r.invoiceNumber = invoiceNumber;
        r.studentName = studentName;
        r.method = f.getMethod();
        r.status = f.getStatus();
        r.amount = f.getAmount();
        r.gatewayTranId = f.getGatewayTranId();
        r.gatewayCardType = f.getGatewayCardType();
        r.failureReason = f.getFailureReason();
        r.payerName = f.getPayerName();
        r.payerEmail = f.getPayerEmail();
        r.payerPhone = f.getPayerPhone();
        r.initiatedBy = f.getInitiatedBy();
        r.initiatedAt = f.getInitiatedAt();
        r.failedAt = f.getFailedAt();
        return r;
    }
}
