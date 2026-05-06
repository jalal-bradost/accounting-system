package com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomType;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditTrack;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditableModel;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditingEntityListener;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.ArchivableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "inv_uom", indexes = {
        @Index(name = "ix_inv_uom_company", columnList = "company_id,active"),
        @Index(name = "ix_inv_uom_category", columnList = "category_id,active")
})
@EntityListeners(AuditingEntityListener.class)
@AuditableModel("inventory.uom")
public class UomEntity extends ArchivableEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false, length = 255)
    @AuditTrack
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "uom_type", nullable = false, length = 20)
    @AuditTrack(name = "uomType")
    private UomType uomType;

    @Column(nullable = false, precision = 19, scale = 6)
    @AuditTrack
    private BigDecimal factor;

    @Column(nullable = false)
    private int rounding;

    public UomEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID v) { this.categoryId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public UomType getUomType() { return uomType; }
    public void setUomType(UomType v) { this.uomType = v; }
    public BigDecimal getFactor() { return factor; }
    public void setFactor(BigDecimal v) { this.factor = v; }
    public int getRounding() { return rounding; }
    public void setRounding(int v) { this.rounding = v; }
}
