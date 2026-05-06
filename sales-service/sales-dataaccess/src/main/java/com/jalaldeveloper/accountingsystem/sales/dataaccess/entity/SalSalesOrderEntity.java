package com.jalaldeveloper.accountingsystem.sales.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.sales.domain.core.SalesOrderDeliveryStatus;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalesOrderInvoiceStatus;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalesOrderState;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sal_sales_order", indexes = {
        @Index(name = "ix_sal_so_company_state", columnList = "company_id,state"),
        @Index(name = "ix_sal_so_customer", columnList = "customer_partner_id")
}, uniqueConstraints = @UniqueConstraint(name = "uk_sal_so_company_name", columnNames = {"company_id", "name"}))
public class SalSalesOrderEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "customer_partner_id", nullable = false)
    private UUID customerPartnerId;

    @Column(nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SalesOrderState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 32)
    private SalesOrderDeliveryStatus deliveryStatus = SalesOrderDeliveryStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_status", nullable = false, length = 32)
    private SalesOrderInvoiceStatus invoiceStatus = SalesOrderInvoiceStatus.NOTHING;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "validity_date")
    private LocalDate validityDate;

    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(name = "pricelist_id")
    private UUID pricelistId;

    @Column(name = "payment_terms_id")
    private UUID paymentTermsId;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "exchange_rate_to_company", precision = 19, scale = 8)
    private BigDecimal exchangeRateToCompany;

    @Column(length = 32)
    private String incoterm;

    @Column(length = 4000)
    private String notes;

    @Column(name = "amount_untaxed", precision = 19, scale = 4, nullable = false)
    private BigDecimal amountUntaxed;

    @Column(name = "amount_tax", precision = 19, scale = 4, nullable = false)
    private BigDecimal amountTax;

    @Column(name = "amount_total", precision = 19, scale = 4, nullable = false)
    private BigDecimal amountTotal;

    @Column(name = "quotation_sent_at")
    private Instant quotationSentAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "delivery_completed_at")
    private Instant deliveryCompletedAt;

    @Column(name = "invoicing_completed_at")
    private Instant invoicingCompletedAt;

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

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequence ASC")
    private List<SalSalesOrderLineEntity> lines = new ArrayList<>();

    public SalSalesOrderEntity() {}

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
    public Instant getQuotationSentAt() { return quotationSentAt; }
    public void setQuotationSentAt(Instant quotationSentAt) { this.quotationSentAt = quotationSentAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
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
    public List<SalSalesOrderLineEntity> getLines() { return lines; }
    public void setLines(List<SalSalesOrderLineEntity> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
    }
}
