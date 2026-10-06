package com.bradox.erp.inventory.dataaccess.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "inv_stock_hold_lock")
@IdClass(StockHoldLockEntity.Pk.class)
public class StockHoldLockEntity {

    @Id
    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Id
    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Id
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    public StockHoldLockEntity() {}

    public StockHoldLockEntity(UUID companyId, UUID warehouseId, UUID productId) {
        this.companyId = companyId;
        this.warehouseId = warehouseId;
        this.productId = productId;
    }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public static class Pk implements Serializable {
        private UUID companyId;
        private UUID warehouseId;
        private UUID productId;

        public Pk() {}

        public Pk(UUID companyId, UUID warehouseId, UUID productId) {
            this.companyId = companyId;
            this.warehouseId = warehouseId;
            this.productId = productId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(companyId, pk.companyId)
                    && Objects.equals(warehouseId, pk.warehouseId)
                    && Objects.equals(productId, pk.productId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(companyId, warehouseId, productId);
        }
    }
}
