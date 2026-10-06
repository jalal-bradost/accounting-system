package com.bradox.erp.inventory.dataaccess.entity;

import com.bradox.erp.inventory.domain.core.valueobject.StockHoldOwnerType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inv_stock_hold", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inv_stock_hold_owner_product",
                columnNames = {"company_id", "owner_type", "owner_id", "product_id"})
}, indexes = {
        @Index(name = "ix_inv_stock_hold_avail", columnList = "company_id,warehouse_id,product_id,expires_at"),
        @Index(name = "ix_inv_stock_hold_expires", columnList = "expires_at"),
        @Index(name = "ix_inv_stock_hold_owner", columnList = "owner_type,owner_id")
})
public class StockHoldEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "holder_id", nullable = false)
    private UUID holderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 32)
    private StockHoldOwnerType ownerType;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public StockHoldEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public UUID getHolderId() { return holderId; }
    public void setHolderId(UUID holderId) { this.holderId = holderId; }
    public StockHoldOwnerType getOwnerType() { return ownerType; }
    public void setOwnerType(StockHoldOwnerType ownerType) { this.ownerType = ownerType; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
