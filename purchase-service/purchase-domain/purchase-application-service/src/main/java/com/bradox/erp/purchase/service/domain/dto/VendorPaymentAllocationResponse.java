package com.bradox.erp.purchase.service.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class VendorPaymentAllocationResponse {
    private UUID id;
    private UUID paymentId;
    private String paymentReference;
    private LocalDateTime paymentDate;
    private UUID vendorBillId;
    private String billReference;
    private String billMoveType;
    private boolean billOpeningBalance;
    private BigDecimal amount;
    private BigDecimal paymentAmount;
    private String currencyCode;
    private BigDecimal amountCompany;
    private BigDecimal paymentAmountCompany;
    private LocalDate allocationDate;
    private UUID fxJournalEntryId;
    private UUID fxReversalJournalEntryId;
    /** ACTIVE | REVERSED */
    private String state;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public UUID getVendorBillId() { return vendorBillId; }
    public void setVendorBillId(UUID vendorBillId) { this.vendorBillId = vendorBillId; }
    public String getBillReference() { return billReference; }
    public void setBillReference(String billReference) { this.billReference = billReference; }
    public String getBillMoveType() { return billMoveType; }
    public void setBillMoveType(String billMoveType) { this.billMoveType = billMoveType; }
    public boolean isBillOpeningBalance() { return billOpeningBalance; }
    public void setBillOpeningBalance(boolean billOpeningBalance) { this.billOpeningBalance = billOpeningBalance; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public BigDecimal getAmountCompany() { return amountCompany; }
    public void setAmountCompany(BigDecimal amountCompany) { this.amountCompany = amountCompany; }
    public BigDecimal getPaymentAmountCompany() { return paymentAmountCompany; }
    public void setPaymentAmountCompany(BigDecimal paymentAmountCompany) { this.paymentAmountCompany = paymentAmountCompany; }
    public LocalDate getAllocationDate() { return allocationDate; }
    public void setAllocationDate(LocalDate allocationDate) { this.allocationDate = allocationDate; }
    public UUID getFxJournalEntryId() { return fxJournalEntryId; }
    public void setFxJournalEntryId(UUID fxJournalEntryId) { this.fxJournalEntryId = fxJournalEntryId; }
    public UUID getFxReversalJournalEntryId() { return fxReversalJournalEntryId; }
    public void setFxReversalJournalEntryId(UUID fxReversalJournalEntryId) { this.fxReversalJournalEntryId = fxReversalJournalEntryId; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
