package com.jalaldeveloper.accountingsystem.pos.service.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class PosConfigCommand {
    private UUID companyId;
    @NotBlank
    private String name;
    @NotNull
    private UUID warehouseId;
    @NotNull
    private UUID defaultCustomerPartnerId;
    @NotNull
    private UUID cashJournalId;
    private UUID bankJournalId;
    private UUID pricelistId;
    @NotBlank
    private String currencyCode;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }
    public UUID getDefaultCustomerPartnerId() { return defaultCustomerPartnerId; }
    public void setDefaultCustomerPartnerId(UUID defaultCustomerPartnerId) { this.defaultCustomerPartnerId = defaultCustomerPartnerId; }
    public UUID getCashJournalId() { return cashJournalId; }
    public void setCashJournalId(UUID cashJournalId) { this.cashJournalId = cashJournalId; }
    public UUID getBankJournalId() { return bankJournalId; }
    public void setBankJournalId(UUID bankJournalId) { this.bankJournalId = bankJournalId; }
    public UUID getPricelistId() { return pricelistId; }
    public void setPricelistId(UUID pricelistId) { this.pricelistId = pricelistId; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
}
