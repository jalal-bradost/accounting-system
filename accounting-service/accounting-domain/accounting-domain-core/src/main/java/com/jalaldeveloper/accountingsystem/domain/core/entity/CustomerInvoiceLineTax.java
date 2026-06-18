package com.jalaldeveloper.accountingsystem.domain.core.entity;

import java.math.BigDecimal;
import java.util.UUID;

public class CustomerInvoiceLineTax {

    private UUID id;
    private UUID taxId;
    private String taxName;
    private BigDecimal taxBase;
    private BigDecimal taxAmount;
    private UUID accountId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
