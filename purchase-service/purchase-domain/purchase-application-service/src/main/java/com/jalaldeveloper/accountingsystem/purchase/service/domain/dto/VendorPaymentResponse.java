package com.jalaldeveloper.accountingsystem.purchase.service.domain.dto;

import com.jalaldeveloper.accountingsystem.purchase.domain.core.VendorPaymentState;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class VendorPaymentResponse {
    private UUID id;
    private UUID companyId;
    private UUID vendorBillId;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private String currencyCode;
    private VendorPaymentState state;
    private UUID journalEntryId;
    private UUID reconciliationId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getVendorBillId() { return vendorBillId; }
    public void setVendorBillId(UUID vendorBillId) { this.vendorBillId = vendorBillId; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public VendorPaymentState getState() { return state; }
    public void setState(VendorPaymentState state) { this.state = state; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; }
    public UUID getReconciliationId() { return reconciliationId; }
    public void setReconciliationId(UUID reconciliationId) { this.reconciliationId = reconciliationId; }
}
