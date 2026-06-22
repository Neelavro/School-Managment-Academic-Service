package com.example.academic_service.dto;

import com.example.academic_service.entity.AccountingSettings;
import com.example.academic_service.entity.ChartOfAccount;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccountingSettingsResponse {
    private Long arAccountId;
    private String arAccountCode;
    private String arAccountName;
    private Long cashAccountId;
    private String cashAccountCode;
    private String cashAccountName;

    private Long gatewayClearingAccountId;
    private String gatewayClearingAccountCode;
    private String gatewayClearingAccountName;

    private Long platformFeeAccountId;
    private String platformFeeAccountCode;
    private String platformFeeAccountName;

    private Long platformPayableAccountId;
    private String platformPayableAccountCode;
    private String platformPayableAccountName;

    private BigDecimal platformFeePercent;
    private BigDecimal platformFeeFlat;

    private Integer invoiceDueDays;
    private String invoiceNumberPrefix;
    private String journalNumberPrefix;

    public static AccountingSettingsResponse from(
            AccountingSettings s,
            ChartOfAccount ar, ChartOfAccount cash,
            ChartOfAccount gatewayClearing, ChartOfAccount platformFee, ChartOfAccount platformPayable) {
        AccountingSettingsResponse r = new AccountingSettingsResponse();
        r.arAccountId = s.getArAccountId();
        if (ar != null) { r.arAccountCode = ar.getAccountCode(); r.arAccountName = ar.getAccountName(); }
        r.cashAccountId = s.getCashAccountId();
        if (cash != null) { r.cashAccountCode = cash.getAccountCode(); r.cashAccountName = cash.getAccountName(); }
        r.gatewayClearingAccountId = s.getGatewayClearingAccountId();
        if (gatewayClearing != null) {
            r.gatewayClearingAccountCode = gatewayClearing.getAccountCode();
            r.gatewayClearingAccountName = gatewayClearing.getAccountName();
        }
        r.platformFeeAccountId = s.getPlatformFeeAccountId();
        if (platformFee != null) {
            r.platformFeeAccountCode = platformFee.getAccountCode();
            r.platformFeeAccountName = platformFee.getAccountName();
        }
        r.platformPayableAccountId = s.getPlatformPayableAccountId();
        if (platformPayable != null) {
            r.platformPayableAccountCode = platformPayable.getAccountCode();
            r.platformPayableAccountName = platformPayable.getAccountName();
        }
        r.platformFeePercent = s.getPlatformFeePercent();
        r.platformFeeFlat = s.getPlatformFeeFlat();
        r.invoiceDueDays = s.getInvoiceDueDays();
        r.invoiceNumberPrefix = s.getInvoiceNumberPrefix();
        r.journalNumberPrefix = s.getJournalNumberPrefix();
        return r;
    }
}
