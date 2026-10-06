package com.bradox.erp.purchase.service.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RegisterVendorPaymentCommand {

    private UUID companyId;
    /**
     * Legacy shorthand: allocate the full amount to this bill / credit note. Mutually exclusive
     * with {@link #allocations}.
     */
    private UUID vendorBillId;
    /** Required when no document is given; must match the documents' vendor otherwise. */
    private UUID vendorPartnerId;
    /** Optional allocations applied right after the payment is posted (sum ≤ amount). */
    @Valid
    private List<VendorPaymentAllocationLine> allocations = new ArrayList<>();
    /** Cash or bank journal; liquidity account is resolved from the journal code. */
    @NotNull
    private UUID bankJournalId;
    @NotNull
    private LocalDateTime paymentDate;
    @NotNull
    @Positive
    private BigDecimal amount;
    @NotNull
    private String currencyCode;
    /** Document × rate = company currency. Defaults from company currency master on payment date. */
    private BigDecimal exchangeRateToCompany;
    private String reference;
    /** Optional override: credit this account instead of the payment journal's liquidity account. */
    private UUID liquidityAccountId;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getVendorBillId() { return vendorBillId; }
    public void setVendorBillId(UUID vendorBillId) { this.vendorBillId = vendorBillId; }
    public UUID getVendorPartnerId() { return vendorPartnerId; }
    public void setVendorPartnerId(UUID vendorPartnerId) { this.vendorPartnerId = vendorPartnerId; }
    public List<VendorPaymentAllocationLine> getAllocations() { return allocations; }
    public void setAllocations(List<VendorPaymentAllocationLine> allocations) {
        this.allocations = allocations != null ? allocations : new ArrayList<>();
    }
    public UUID getBankJournalId() { return bankJournalId; }
    public void setBankJournalId(UUID bankJournalId) { this.bankJournalId = bankJournalId; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public BigDecimal getExchangeRateToCompany() { return exchangeRateToCompany; }
    public void setExchangeRateToCompany(BigDecimal exchangeRateToCompany) { this.exchangeRateToCompany = exchangeRateToCompany; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public UUID getLiquidityAccountId() { return liquidityAccountId; }
    public void setLiquidityAccountId(UUID liquidityAccountId) { this.liquidityAccountId = liquidityAccountId; }
}
