package com.bradox.erp.sales.domain.core.entity;

import com.bradox.erp.domain.valueobject.DiscountType;
import com.bradox.erp.sales.domain.core.SalesOrderDeliveryStatus;
import com.bradox.erp.sales.domain.core.SalesOrderInvoiceStatus;
import com.bradox.erp.sales.domain.core.SalesOrderState;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SalesOrder {

    private UUID id;
    private UUID companyId;
    private UUID customerPartnerId;
    private String name;
    private SalesOrderState state;
    private SalesOrderDeliveryStatus deliveryStatus = SalesOrderDeliveryStatus.PENDING;
    private SalesOrderInvoiceStatus invoiceStatus = SalesOrderInvoiceStatus.NOTHING;
    private LocalDate orderDate;
    private LocalDate validityDate;
    private UUID warehouseId;
    private UUID pricelistId;
    private UUID paymentTermsId;
    private String currencyCode;
    private BigDecimal exchangeRateToCompany;
    private String incoterm;
    private String notes;
    private BigDecimal amountUntaxed;
    private BigDecimal amountTax;
    private BigDecimal amountTotal;
    /** Order-level discount as entered. {@code orderDiscountPercent} is derived for reporting. */
    private DiscountType orderDiscountType = DiscountType.PERCENT;
    private BigDecimal orderDiscountValue = BigDecimal.ZERO;
    private BigDecimal orderDiscountPercent = BigDecimal.ZERO;
    private Instant quotationSentAt;
    private Instant confirmedAt;
    private Instant cancelledAt;
    /** When true, confirmed order cannot be amended or cancelled until unlocked. */
    private boolean locked;
    private Instant deliveryCompletedAt;
    private Instant invoicingCompletedAt;
    private long rowVersion;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private List<SalesOrderLine> lines = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getCustomerPartnerId() { return customerPartnerId; }
    public void setCustomerPartnerId(UUID customerPartnerId) { this.customerPartnerId = customerPartnerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public SalesOrderState getState() { return state; }
    public void setState(SalesOrderState state) { this.state = state; }
    public SalesOrderDeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(SalesOrderDeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public SalesOrderInvoiceStatus getInvoiceStatus() { return invoiceStatus; }
    public void setInvoiceStatus(SalesOrderInvoiceStatus invoiceStatus) { this.invoiceStatus = invoiceStatus; }
    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    public LocalDate getValidityDate() { return validityDate; }
    public void setValidityDate(LocalDate validityDate) { this.validityDate = validityDate; }
    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }
    public UUID getPricelistId() { return pricelistId; }
    public void setPricelistId(UUID pricelistId) { this.pricelistId = pricelistId; }
    public UUID getPaymentTermsId() { return paymentTermsId; }
    public void setPaymentTermsId(UUID paymentTermsId) { this.paymentTermsId = paymentTermsId; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public BigDecimal getExchangeRateToCompany() { return exchangeRateToCompany; }
    public void setExchangeRateToCompany(BigDecimal exchangeRateToCompany) { this.exchangeRateToCompany = exchangeRateToCompany; }
    public String getIncoterm() { return incoterm; }
    public void setIncoterm(String incoterm) { this.incoterm = incoterm; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
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
    public Instant getQuotationSentAt() { return quotationSentAt; }
    public void setQuotationSentAt(Instant quotationSentAt) { this.quotationSentAt = quotationSentAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public Instant getDeliveryCompletedAt() { return deliveryCompletedAt; }
    public void setDeliveryCompletedAt(Instant deliveryCompletedAt) { this.deliveryCompletedAt = deliveryCompletedAt; }
    public Instant getInvoicingCompletedAt() { return invoicingCompletedAt; }
    public void setInvoicingCompletedAt(Instant invoicingCompletedAt) { this.invoicingCompletedAt = invoicingCompletedAt; }
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
    public List<SalesOrderLine> getLines() { return lines; }
    public void setLines(List<SalesOrderLine> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
    }
}
