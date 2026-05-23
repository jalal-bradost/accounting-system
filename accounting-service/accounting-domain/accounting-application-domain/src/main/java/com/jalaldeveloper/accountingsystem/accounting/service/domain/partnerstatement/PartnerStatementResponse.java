package com.jalaldeveloper.accountingsystem.accounting.service.domain.partnerstatement;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Activity for a partner: optional receivable (customer invoices and receipts) and/or
 * payable (vendor bills and payments) sections, depending on partner roles and permissions.
 * Each section is scoped to one document currency so running balances never mix FX.
 */
public class PartnerStatementResponse {

    private UUID partnerId;
    private String partnerDisplayName;
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<PartnerStatementSectionResponse> receivableSections = new ArrayList<>();
    private List<PartnerStatementSectionResponse> payableSections = new ArrayList<>();

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

    public List<PartnerStatementSectionResponse> getReceivableSections() {
        return receivableSections;
    }

    public void setReceivableSections(List<PartnerStatementSectionResponse> receivableSections) {
        this.receivableSections = receivableSections != null ? receivableSections : new ArrayList<>();
    }

    public List<PartnerStatementSectionResponse> getPayableSections() {
        return payableSections;
    }

    public void setPayableSections(List<PartnerStatementSectionResponse> payableSections) {
        this.payableSections = payableSections != null ? payableSections : new ArrayList<>();
    }
}
