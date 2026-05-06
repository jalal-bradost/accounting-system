package com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pur_vendor_bill_line_tax")
public class PurVendorBillLineTaxEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pur_vblt_line"))
    private PurVendorBillLineEntity line;

    @Column(name = "tax_id", nullable = false)
    private UUID taxId;

    @Column(name = "tax_name", nullable = false, length = 255)
    private String taxName;

    @Column(name = "tax_base", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxBase;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    public PurVendorBillLineTaxEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PurVendorBillLineEntity getLine() { return line; }
    public void setLine(PurVendorBillLineEntity line) { this.line = line; }
    public UUID getTaxId() { return taxId; }
    public void setTaxId(UUID taxId) { this.taxId = taxId; }
    public String getTaxName() { return taxName; }
    public void setTaxName(String taxName) { this.taxName = taxName; }
    public BigDecimal getTaxBase() { return taxBase; }
    public void setTaxBase(BigDecimal taxBase) { this.taxBase = taxBase; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
}
