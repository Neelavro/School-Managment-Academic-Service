package com.example.academic_service.dto;

import com.example.academic_service.entity.Voucher;
import com.example.academic_service.entity.VoucherType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class VoucherResponse {
    private Long id;
    private String voucherNumber;
    private VoucherType voucherType;
    private LocalDate voucherDate;
    private String narration;
    private String partyName;
    private BigDecimal totalAmount;
    private Long journalEntryId;
    private String journalEntryNumber;
    private Boolean isReversed;
    private Long reversesVoucherId;
    private Long reversedByVoucherId;
    private String createdBy;
    private LocalDateTime createdAt;
    /** Hydrated for detail view; omitted in list view to keep payloads light. */
    private JournalEntryResponse journalEntry;

    public static VoucherResponse from(Voucher v, String journalEntryNumber, JournalEntryResponse je) {
        VoucherResponse r = new VoucherResponse();
        r.id = v.getId();
        r.voucherNumber = v.getVoucherNumber();
        r.voucherType = v.getVoucherType();
        r.voucherDate = v.getVoucherDate();
        r.narration = v.getNarration();
        r.partyName = v.getPartyName();
        r.totalAmount = v.getTotalAmount();
        r.journalEntryId = v.getJournalEntryId();
        r.journalEntryNumber = journalEntryNumber;
        r.isReversed = v.getIsReversed();
        r.reversesVoucherId = v.getReversesVoucherId();
        r.reversedByVoucherId = v.getReversedByVoucherId();
        r.createdBy = v.getCreatedBy();
        r.createdAt = v.getCreatedAt();
        r.journalEntry = je;
        return r;
    }
}
