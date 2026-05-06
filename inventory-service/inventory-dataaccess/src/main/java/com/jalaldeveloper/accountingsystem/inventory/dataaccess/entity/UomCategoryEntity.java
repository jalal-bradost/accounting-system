package com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.platform.audit.AuditTrack;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditableModel;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditingEntityListener;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.ArchivableEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "inv_uom_category", indexes = {
        @Index(name = "ix_inv_uomcat_company", columnList = "company_id,active")
})
@EntityListeners(AuditingEntityListener.class)
@AuditableModel("inventory.uom.category")
public class UomCategoryEntity extends ArchivableEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 255)
    @AuditTrack
    private String name;

    public UomCategoryEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
}
