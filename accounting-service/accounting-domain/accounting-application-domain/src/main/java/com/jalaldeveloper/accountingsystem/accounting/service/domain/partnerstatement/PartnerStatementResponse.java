package com.jalaldeveloper.accountingsystem.accounting.service.domain.partnerstatement;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Activity for a partner: optional receivable (customer invoices and receipts) and/or
 * payable (vendor bills and payments) sections, depending on partner roles and permissions.
 */
public class PartnerStatementResponse {

    private UUID partnerId;
    private String partnerDisplayName;
    private LocalDate fromDate;
    private LocalDate toDate;
    private PartnerStatementSectionResponse receivable;
    private PartnerStatementSectionResponse payable;

    public UUID getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(UUID partnerId) {
        this.partnerId = partnerId;
    }

    public String getPartnerDisplayName() {
        return partnerDisplayName;
    }

    public void setPartnerDisplayName(String partnerDisplayName) {
        this.partnerDisplayName = partnerDisplayName;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public PartnerStatementSectionResponse getReceivable() {
        return receivable;
    }

    public void setReceivable(PartnerStatementSectionResponse receivable) {
        this.receivable = receivable;
    }

    public PartnerStatementSectionResponse getPayable() {
        return payable;
    }

    public void setPayable(PartnerStatementSectionResponse payable) {
        this.payable = payable;
    }
}
