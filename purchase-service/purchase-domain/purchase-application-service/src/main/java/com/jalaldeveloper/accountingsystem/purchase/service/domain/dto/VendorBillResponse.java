package com.jalaldeveloper.accountingsystem.purchase.service.domain.dto;

import com.jalaldeveloper.accountingsystem.purchase.domain.core.VendorBillMoveType;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.VendorBillState;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class VendorBillResponse {
    private UUID id;
    private UUID companyId;
    private UUID vendorPartnerId;
    private UUID purchaseOrderId;
    private LocalDate billDate;
    private LocalDate dueDate;
    private String reference;
    private String currencyCode;
    private VendorBillState state;
    private VendorBillMoveType moveType;
    private UUID reversedBillId;
    private UUID journalEntryId;
    private List<VendorBillLineResponse> lines;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getVendorPartnerId() { return vendorPartnerId; }
    public void setVendorPartnerId(UUID vendorPartnerId) { this.vendorPartnerId = vendorPartnerId; }
    public UUID getPurchaseOrderId() { return purchaseOrderId; }
    public void setPurchaseOrderId(UUID purchaseOrderId) { this.purchaseOrderId = purchaseOrderId; }
    public LocalDate getBillDate() { return billDate; }
    public void setBillDate(LocalDate billDate) { this.billDate = billDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public VendorBillState getState() { return state; }
    public void setState(VendorBillState state) { this.state = state; }
    public VendorBillMoveType getMoveType() { return moveType; }
    public void setMoveType(VendorBillMoveType moveType) { this.moveType = moveType; }
    public UUID getReversedBillId() { return reversedBillId; }
    public void setReversedBillId(UUID reversedBillId) { this.reversedBillId = reversedBillId; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; }
    public List<VendorBillLineResponse> getLines() { return lines; }
    public void setLines(List<VendorBillLineResponse> lines) { this.lines = lines; }
}
