package com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * On-hand and reserved quantity for a (company, product, location) tuple. Concurrent updates
 * are protected by an optimistic lock (JPA {@code @Version}).
 */
@Entity
@Table(name = "inv_stock_quant", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inv_quant_clp", columnNames = {"company_id", "product_id", "location_id"})
}, indexes = {
        @Index(name = "ix_inv_quant_company_product", columnList = "company_id,product_id"),
        @Index(name = "ix_inv_quant_company_location", columnList = "company_id,location_id")
})
public class StockQuantEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "reserved_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedQuantity;

    @Column(name = "last_changed_at")
    private Instant lastChangedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public StockQuantEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
    public Instant getLastChangedAt() { return lastChangedAt; }
    public void setLastChangedAt(Instant v) { this.lastChangedAt = v; }
    public long getVersion() { return version; }
    public void setVersion(long v) { this.version = v; }
}
