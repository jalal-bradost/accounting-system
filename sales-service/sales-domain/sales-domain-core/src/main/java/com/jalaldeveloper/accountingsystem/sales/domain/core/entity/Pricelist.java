package com.jalaldeveloper.accountingsystem.sales.domain.core.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Pricelist {

    private UUID id;
    private UUID companyId;
    private String name;
    private String currencyCode;
    private boolean active = true;
    private int sequence;
    private Instant createdAt;
    private Instant updatedAt;
    private List<PricelistItem> items = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<PricelistItem> getItems() { return items; }
    public void setItems(List<PricelistItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}
