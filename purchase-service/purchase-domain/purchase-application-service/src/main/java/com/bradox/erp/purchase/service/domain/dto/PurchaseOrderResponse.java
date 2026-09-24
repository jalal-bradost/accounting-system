package com.bradox.erp.purchase.service.domain.dto;

import com.bradox.erp.purchase.domain.core.PurchaseOrderState;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class PurchaseOrderResponse {
    private UUID id;
    private UUID companyId;
    private UUID vendorPartnerId;
    private String name;
    private PurchaseOrderState state;
    private String currencyCode;
    private UUID warehouseId;
    private UUID destLocationId;
    private UUID paymentTermsId;
    private LocalDate orderDate;
    private LocalDate expectedDate;
    private String incoterm;
    private String notes;
    private String vendorReference;
    private BigDecimal amountUntaxed;
    private BigDecimal amountTax;
    private BigDecimal amountTotal;
    private com.bradox.erp.domain.valueobject.DiscountType orderDiscountType;
    private BigDecimal orderDiscountValue;
    private BigDecimal orderDiscountPercent;
    private BigDecimal amountPaid;
    private BigDecimal amountDue;
    private String paymentStatus;
    private BigDecimal exchangeRateToCompany;
    private Instant sentAt;
    private Instant confirmedAt;
    private Instant cancelledAt;
    private boolean locked;
    /** True when the API would allow creating a vendor bill (confirmed + billable quantity on at least one line). */
    private boolean canCreateVendorBill;
    private boolean canCreateReturn;
    private List<UUID> receiptPickingIds;
    private List<UUID> returnPickingIds;
    private List<PurchaseOrderLineResponse> lines;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getVendorPartnerId() { return vendorPartnerId; }
    public void setVendorPartnerId(UUID vendorPartnerId) { this.vendorPartnerId = vendorPartnerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PurchaseOrderState getState() { return state; }
    public void setState(PurchaseOrderState state) { this.state = state; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }
    public UUID getDestLocationId() { return destLocationId; }
    public void setDestLocationId(UUID destLocationId) { this.destLocationId = destLocationId; }
    public UUID getPaymentTermsId() { return paymentTermsId; }
    public void setPaymentTermsId(UUID paymentTermsId) { this.paymentTermsId = paymentTermsId; }
    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    public LocalDate getExpectedDate() { return expectedDate; }
    public void setExpectedDate(LocalDate expectedDate) { this.expectedDate = expectedDate; }
    public String getIncoterm() { return incoterm; }
    public void setIncoterm(String incoterm) { this.incoterm = incoterm; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getVendorReference() { return vendorReference; }
    public void setVendorReference(String vendorReference) { this.vendorReference = vendorReference; }
    public BigDecimal getAmountUntaxed() { return amountUntaxed; }
    public void setAmountUntaxed(BigDecimal amountUntaxed) { this.amountUntaxed = amountUntaxed; }
    public BigDecimal getAmountTax() { return amountTax; }
    public void setAmountTax(BigDecimal amountTax) { this.amountTax = amountTax; }
    public BigDecimal getAmountTotal() { return amountTotal; }
    public void setAmountTotal(BigDecimal amountTotal) { this.amountTotal = amountTotal; }
    public com.bradox.erp.domain.valueobject.DiscountType getOrderDiscountType() { return orderDiscountType; }
    public void setOrderDiscountType(com.bradox.erp.domain.valueobject.DiscountType orderDiscountType) {
        this.orderDiscountType = orderDiscountType;
    }
    public BigDecimal getOrderDiscountValue() { return orderDiscountValue; }
    public void setOrderDiscountValue(BigDecimal orderDiscountValue) { this.orderDiscountValue = orderDiscountValue; }
    public BigDecimal getOrderDiscountPercent() { return orderDiscountPercent; }
    public void setOrderDiscountPercent(BigDecimal orderDiscountPercent) { this.orderDiscountPercent = orderDiscountPercent; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
    public BigDecimal getAmountDue() { return amountDue; }
    public void setAmountDue(BigDecimal amountDue) { this.amountDue = amountDue; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public BigDecimal getExchangeRateToCompany() { return exchangeRateToCompany; }
    public void setExchangeRateToCompany(BigDecimal exchangeRateToCompany) { this.exchangeRateToCompany = exchangeRateToCompany; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public boolean isCanCreateVendorBill() { return canCreateVendorBill; }
    public void setCanCreateVendorBill(boolean canCreateVendorBill) { this.canCreateVendorBill = canCreateVendorBill; }
    public boolean isCanCreateReturn() { return canCreateReturn; }
    public void setCanCreateReturn(boolean canCreateReturn) { this.canCreateReturn = canCreateReturn; }
    public List<UUID> getReceiptPickingIds() { return receiptPickingIds; }
    public void setReceiptPickingIds(List<UUID> receiptPickingIds) { this.receiptPickingIds = receiptPickingIds; }
    public List<UUID> getReturnPickingIds() { return returnPickingIds; }
    public void setReturnPickingIds(List<UUID> returnPickingIds) { this.returnPickingIds = returnPickingIds; }
    public List<PurchaseOrderLineResponse> getLines() { return lines; }
    public void setLines(List<PurchaseOrderLineResponse> lines) { this.lines = lines; }
}
