package com.example.academic_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Two-line voucher (Receipt, Payment, Contra) where one side is the Cash/Bank
 * account and the other side is the contra account. Used by:
 *
 *  RECEIPT — cashOrBankAccountId is the Cash/Bank you received money INTO;
 *            contraAccountId is the income/source.
 *            Posts: Dr cashOrBank / Cr contra
 *
 *  PAYMENT — cashOrBankAccountId is the Cash/Bank you paid money OUT OF;
 *            contraAccountId is the expense/payee account.
 *            Posts: Dr contra / Cr cashOrBank
 *
 *  CONTRA  — cashOrBankAccountId is the SOURCE asset you moved money FROM;
 *            contraAccountId is the DESTINATION asset.
 *            Posts: Dr contra (destination) / Cr cashOrBank (source)
 *            Both must be ASSET; system enforces it.
 */
@Getter
@Setter
public class SimpleVoucherRequest {

    @NotNull
    private LocalDate voucherDate;

    @NotNull
    private Long cashOrBankAccountId;

    @NotNull
    private Long contraAccountId;

    @NotNull
    @Positive(message = "amount must be > 0")
    private BigDecimal amount;

    private String narration;

    /** Optional free-text party name (payee / payer / bank). */
    private String partyName;
}
