package com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.platform.audit.AuditTrack;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditableModel;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditingEntityListener;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.ArchivableEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "inv_warehouse", indexes = {
        @Index(name = "ix_inv_wh_company", columnList = "company_id,active"),
        @Index(name = "ix_inv_wh_company_code", columnList = "company_id,code")
})
@EntityListeners(AuditingEntityListener.class)
@AuditableModel("inventory.warehouse")
public class WarehouseEntity extends ArchivableEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 50)
    @AuditTrack
    private String code;

    @Column(nullable = false, length = 255)
    @AuditTrack
    private String name;

    @Column(name = "stock_location_id")
    private UUID stockLocationId;

    @Column(name = "input_location_id")
    private UUID inputLocationId;

    @Column(name = "output_location_id")
    private UUID outputLocationId;

    public WarehouseEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public UUID getStockLocationId() { return stockLocationId; }
    public void setStockLocationId(UUID v) { this.stockLocationId = v; }
    public UUID getInputLocationId() { return inputLocationId; }
    public void setInputLocationId(UUID v) { this.inputLocationId = v; }
    public UUID getOutputLocationId() { return outputLocationId; }
    public void setOutputLocationId(UUID v) { this.outputLocationId = v; }
}
