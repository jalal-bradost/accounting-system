package com.bradox.delin.purchase.domain.core.entity;

import com.bradox.delin.domain.valueobject.DiscountType;
import com.bradox.delin.purchase.domain.core.PurchaseOrderState;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PurchaseOrder {

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
    private Instant sentAt;
    private Instant confirmedAt;
    private Instant receivedCompletedAt;
    private Instant billedCompletedAt;
    private Instant cancelledAt;
    /** When true, confirmed order cannot be amended or cancelled until unlocked. */
    private boolean locked;
    private BigDecimal amountUntaxed;
    private BigDecimal amountTax;
    private BigDecimal amountTotal;
    /** Order-level discount as entered. {@code orderDiscountPercent} is derived for reporting. */
    private DiscountType orderDiscountType = DiscountType.PERCENT;
    private BigDecimal orderDiscountValue = BigDecimal.ZERO;
    private BigDecimal orderDiscountPercent = BigDecimal.ZERO;
    private BigDecimal exchangeRateToCompany;
    private long rowVersion;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private List<PurchaseOrderLine> lines = new ArrayList<>();

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
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
    public Instant getReceivedCompletedAt() { return receivedCompletedAt; }
    public void setReceivedCompletedAt(Instant receivedCompletedAt) { this.receivedCompletedAt = receivedCompletedAt; }
    public Instant getBilledCompletedAt() { return billedCompletedAt; }
    public void setBilledCompletedAt(Instant billedCompletedAt) { this.billedCompletedAt = billedCompletedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public BigDecimal getAmountUntaxed() { return amountUntaxed; }
    public void setAmountUntaxed(BigDecimal amountUntaxed) { this.amountUntaxed = amountUntaxed; }
    public BigDecimal getAmountTax() { return amountTax; }
    public void setAmountTax(BigDecimal amountTax) { this.amountTax = amountTax; }
    public BigDecimal getAmountTotal() { return amountTotal; }
    public void setAmountTotal(BigDecimal amountTotal) { this.amountTotal = amountTotal; }
    public DiscountType getOrderDiscountType() { return orderDiscountType; }
    public void setOrderDiscountType(DiscountType orderDiscountType) {
        this.orderDiscountType = DiscountType.orPercent(orderDiscountType);
    }
    public BigDecimal getOrderDiscountValue() { return orderDiscountValue; }
    public void setOrderDiscountValue(BigDecimal orderDiscountValue) {
        this.orderDiscountValue = orderDiscountValue != null ? orderDiscountValue : BigDecimal.ZERO;
    }
    public BigDecimal getOrderDiscountPercent() { return orderDiscountPercent; }
    public void setOrderDiscountPercent(BigDecimal orderDiscountPercent) { this.orderDiscountPercent = orderDiscountPercent; }
    public BigDecimal getExchangeRateToCompany() { return exchangeRateToCompany; }
    public void setExchangeRateToCompany(BigDecimal exchangeRateToCompany) { this.exchangeRateToCompany = exchangeRateToCompany; }
    public long getRowVersion() { return rowVersion; }
    public void setRowVersion(long rowVersion) { this.rowVersion = rowVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public List<PurchaseOrderLine> getLines() { return lines; }
    public void setLines(List<PurchaseOrderLine> lines) { this.lines = lines != null ? lines : new ArrayList<>(); }
}
