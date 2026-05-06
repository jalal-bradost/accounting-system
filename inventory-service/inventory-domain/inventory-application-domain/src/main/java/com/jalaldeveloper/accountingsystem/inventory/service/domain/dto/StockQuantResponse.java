package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class StockQuantResponse {
    private UUID id;
    private UUID companyId;
    private UUID productId;
    private UUID locationId;
    private BigDecimal quantity;
    private BigDecimal reservedQuantity;
    private BigDecimal availableQuantity;
    private Instant lastChangedAt;

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID v) { this.productId = v; }
    public UUID getLocationId() { return locationId; }
    public void setLocationId(UUID v) { this.locationId = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public BigDecimal getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(BigDecimal v) { this.reservedQuantity = v; }
    public BigDecimal getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(BigDecimal v) { this.availableQuantity = v; }
    public Instant getLastChangedAt() { return lastChangedAt; }
    public void setLastChangedAt(Instant v) { this.lastChangedAt = v; }
}
