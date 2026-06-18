package com.jalaldeveloper.accountingsystem.pos.domain.core.entity;

import java.time.Instant;
import java.util.UUID;

public class PosConfig {
    private UUID id;
    private UUID companyId;
    private String name;
    private UUID warehouseId;
    private UUID defaultCustomerPartnerId;
    private UUID cashJournalId;
    private UUID bankJournalId;
    private UUID pricelistId;
    private String currencyCode;
    private boolean active = true;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
