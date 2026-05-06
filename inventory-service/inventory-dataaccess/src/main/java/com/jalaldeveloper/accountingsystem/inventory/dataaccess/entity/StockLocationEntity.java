package com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.LocationType;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditTrack;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditableModel;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditingEntityListener;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.ArchivableEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "inv_stock_location", indexes = {
        @Index(name = "ix_inv_loc_company", columnList = "company_id,active"),
        @Index(name = "ix_inv_loc_warehouse", columnList = "warehouse_id,active"),
        @Index(name = "ix_inv_loc_type", columnList = "company_id,location_type")
})
@EntityListeners(AuditingEntityListener.class)
@AuditableModel("inventory.stock.location")
public class StockLocationEntity extends ArchivableEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 100)
    @AuditTrack
    private String code;

    @Column(nullable = false, length = 255)
    @AuditTrack
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 20)
    @AuditTrack(name = "locationType")
    private LocationType locationType;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(name = "allow_negative_stock", nullable = false)
    @AuditTrack(name = "allowNegativeStock")
    private boolean allowNegativeStock;

    public StockLocationEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public LocationType getLocationType() { return locationType; }
    public void setLocationType(LocationType v) { this.locationType = v; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID v) { this.parentId = v; }
    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID v) { this.warehouseId = v; }
    public boolean isAllowNegativeStock() { return allowNegativeStock; }
    public void setAllowNegativeStock(boolean v) { this.allowNegativeStock = v; }
}
