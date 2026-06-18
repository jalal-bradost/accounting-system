package com.jalaldeveloper.accountingsystem.pos.domain.core.entity;

import com.jalaldeveloper.accountingsystem.pos.domain.core.PosSessionState;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PosSession {
    private UUID id;
    private UUID companyId;
    private UUID configId;
    private PosSessionState state;
    private UUID warehouseId;
    private UUID defaultCustomerPartnerId;
    private UUID cashJournalId;
    private UUID bankJournalId;
    private UUID pricelistId;
    private String currencyCode;
    private BigDecimal openingCash;
    private BigDecimal closingCash;
    private Instant openedAt;
    private Instant closedAt;
    private long rowVersion;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getConfigId() { return configId; }
    public void setConfigId(UUID configId) { this.configId = configId; }
    public PosSessionState getState() { return state; }
    public void setState(PosSessionState state) { this.state = state; }
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
    public BigDecimal getOpeningCash() { return openingCash; }
    public void setOpeningCash(BigDecimal openingCash) { this.openingCash = openingCash; }
    public BigDecimal getClosingCash() { return closingCash; }
    public void setClosingCash(BigDecimal closingCash) { this.closingCash = closingCash; }
    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public long getRowVersion() { return rowVersion; }
    public void setRowVersion(long rowVersion) { this.rowVersion = rowVersion; }
}
