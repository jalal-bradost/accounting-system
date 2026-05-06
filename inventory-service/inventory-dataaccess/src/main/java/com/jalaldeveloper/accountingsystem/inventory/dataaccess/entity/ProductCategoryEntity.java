package com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationMethod;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditTrack;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditableModel;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditingEntityListener;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.ArchivableEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "inv_product_category", indexes = {
        @Index(name = "ix_inv_pcat_company", columnList = "company_id,active")
})
@EntityListeners(AuditingEntityListener.class)
@AuditableModel("inventory.product.category")
public class ProductCategoryEntity extends ArchivableEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 255)
    @AuditTrack
    private String name;

    @Column(name = "parent_id")
    private UUID parentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "valuation_method", nullable = false, length = 20)
    @AuditTrack(name = "valuationMethod")
    private ValuationMethod valuationMethod;

    @Column(name = "stock_valuation_account_id")
    private UUID stockValuationAccountId;

    @Column(name = "stock_input_account_id")
    private UUID stockInputAccountId;

    @Column(name = "stock_output_account_id")
    private UUID stockOutputAccountId;

    @Column(name = "cogs_account_id")
    private UUID cogsAccountId;

    public ProductCategoryEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID v) { this.parentId = v; }
    public ValuationMethod getValuationMethod() { return valuationMethod; }
    public void setValuationMethod(ValuationMethod v) { this.valuationMethod = v; }
    public UUID getStockValuationAccountId() { return stockValuationAccountId; }
    public void setStockValuationAccountId(UUID v) { this.stockValuationAccountId = v; }
    public UUID getStockInputAccountId() { return stockInputAccountId; }
    public void setStockInputAccountId(UUID v) { this.stockInputAccountId = v; }
    public UUID getStockOutputAccountId() { return stockOutputAccountId; }
    public void setStockOutputAccountId(UUID v) { this.stockOutputAccountId = v; }
    public UUID getCogsAccountId() { return cogsAccountId; }
    public void setCogsAccountId(UUID v) { this.cogsAccountId = v; }
}
