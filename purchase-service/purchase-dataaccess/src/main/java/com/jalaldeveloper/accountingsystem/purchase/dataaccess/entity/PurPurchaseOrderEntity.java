package com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.purchase.domain.core.PurchaseOrderState;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pur_purchase_order", indexes = {
        @Index(name = "ix_pur_po_company_state", columnList = "company_id,state"),
        @Index(name = "ix_pur_po_vendor", columnList = "vendor_partner_id,order_date")
}, uniqueConstraints = @UniqueConstraint(name = "uk_pur_po_company_name", columnNames = {"company_id", "name"}))
public class PurPurchaseOrderEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "vendor_partner_id", nullable = false)
    private UUID vendorPartnerId;

    @Column(nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseOrderState state;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(name = "dest_location_id")
    private UUID destLocationId;

    @Column(name = "payment_terms_id")
    private UUID paymentTermsId;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(length = 32)
    private String incoterm;

    @Column(length = 4000)
    private String notes;

    @Column(name = "vendor_reference", length = 255)
    private String vendorReference;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "received_completed_at")
    private Instant receivedCompletedAt;

    @Column(name = "billed_completed_at")
    private Instant billedCompletedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "amount_untaxed", precision = 19, scale = 4, nullable = false)
    private BigDecimal amountUntaxed;

    @Column(name = "amount_tax", precision = 19, scale = 4, nullable = false)
    private BigDecimal amountTax;

    @Column(name = "amount_total", precision = 19, scale = 4, nullable = false)
    private BigDecimal amountTotal;

    @Column(name = "exchange_rate_to_company", precision = 19, scale = 8)
    private BigDecimal exchangeRateToCompany;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequence ASC")
    private List<PurPurchaseOrderLineEntity> lines = new ArrayList<>();

    public PurPurchaseOrderEntity() {}

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
    public BigDecimal getAmountUntaxed() { return amountUntaxed; }
    public void setAmountUntaxed(BigDecimal amountUntaxed) { this.amountUntaxed = amountUntaxed; }
    public BigDecimal getAmountTax() { return amountTax; }
    public void setAmountTax(BigDecimal amountTax) { this.amountTax = amountTax; }
    public BigDecimal getAmountTotal() { return amountTotal; }
    public void setAmountTotal(BigDecimal amountTotal) { this.amountTotal = amountTotal; }
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
    public List<PurPurchaseOrderLineEntity> getLines() { return lines; }
    public void setLines(List<PurPurchaseOrderLineEntity> lines) { this.lines = lines != null ? lines : new ArrayList<>(); }
}
