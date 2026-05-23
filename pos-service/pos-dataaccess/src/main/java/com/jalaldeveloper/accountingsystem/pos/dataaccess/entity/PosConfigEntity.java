package com.jalaldeveloper.accountingsystem.pos.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pos_config", indexes = {
        @Index(name = "ix_pos_config_company_active", columnList = "company_id,active")
}, uniqueConstraints = @UniqueConstraint(name = "uk_pos_config_company_name", columnNames = {"company_id", "name"}))
public class PosConfigEntity {
    @Id
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(nullable = false, length = 128)
    private String name;
    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;
    @Column(name = "default_customer_partner_id", nullable = false)
    private UUID defaultCustomerPartnerId;
    @Column(name = "cash_journal_id", nullable = false)
    private UUID cashJournalId;
    @Column(name = "bank_journal_id")
    private UUID bankJournalId;
    @Column(name = "pricelist_id")
    private UUID pricelistId;
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
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
