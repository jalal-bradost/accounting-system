package com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.purchase.domain.core.VendorBillMoveType;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.VendorBillState;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pur_vendor_bill", indexes = {
        @Index(name = "ix_pur_vb_company_state", columnList = "company_id,state"),
        @Index(name = "ix_pur_vb_po", columnList = "purchase_order_id")
})
public class PurVendorBillEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "vendor_partner_id", nullable = false)
    private UUID vendorPartnerId;

    @Column(name = "purchase_order_id")
    private UUID purchaseOrderId;

    @Column(name = "bill_date", nullable = false)
    private LocalDate billDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(length = 255)
    private String reference;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VendorBillState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "move_type", nullable = false, length = 32)
    private VendorBillMoveType moveType = VendorBillMoveType.BILL;

    @Column(name = "reversed_bill_id")
    private UUID reversedBillId;

    @Column(name = "journal_entry_id")
    private UUID journalEntryId;

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

    @OneToMany(mappedBy = "vendorBill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequence ASC")
    private List<PurVendorBillLineEntity> lines = new ArrayList<>();

    public PurVendorBillEntity() {}

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
    public void setMoveType(VendorBillMoveType moveType) {
        this.moveType = moveType != null ? moveType : VendorBillMoveType.BILL;
    }
    public UUID getReversedBillId() { return reversedBillId; }
    public void setReversedBillId(UUID reversedBillId) { this.reversedBillId = reversedBillId; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; }
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
    public List<PurVendorBillLineEntity> getLines() { return lines; }
    public void setLines(List<PurVendorBillLineEntity> lines) { this.lines = lines != null ? lines : new ArrayList<>(); }
}
