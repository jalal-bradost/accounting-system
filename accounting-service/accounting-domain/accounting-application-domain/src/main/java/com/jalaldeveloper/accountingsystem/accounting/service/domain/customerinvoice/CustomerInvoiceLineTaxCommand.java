package com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class CustomerInvoiceLineTaxCommand {

    @NotNull private UUID taxId;
    @NotNull private String taxName;
    @NotNull private BigDecimal taxBase;
    @NotNull private BigDecimal taxAmount;
    @NotNull private UUID accountId;

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
