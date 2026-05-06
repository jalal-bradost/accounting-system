package com.jalaldeveloper.accountingsystem.dataaccess.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "acc_customer_invoice_line_tax")
public class AccCustomerInvoiceLineTaxEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_id", nullable = false, foreignKey = @ForeignKey(name = "fk_acc_cilt_line"))
    private AccCustomerInvoiceLineEntity line;

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

    public AccCustomerInvoiceLineTaxEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public AccCustomerInvoiceLineEntity getLine() { return line; }
    public void setLine(AccCustomerInvoiceLineEntity line) { this.line = line; }
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
