package com.jalaldeveloper.accountingsystem.pos.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.pos.domain.core.PosSessionState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pos_session", indexes = {
        @Index(name = "ix_pos_session_company_state", columnList = "company_id,state"),
        @Index(name = "ix_pos_session_config", columnList = "config_id")
})
public class PosSessionEntity {
    @Id
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "config_id", nullable = false)
    private UUID configId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PosSessionState state;
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
    @Column(name = "opening_cash", nullable = false, precision = 19, scale = 4)
    private BigDecimal openingCash;
    @Column(name = "closing_cash", precision = 19, scale = 4)
    private BigDecimal closingCash;
    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;
    @Column(name = "closed_at")
    private Instant closedAt;
    @Version
    @Column(name = "row_version", nullable = false)
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
