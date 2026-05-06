package com.jalaldeveloper.accountingsystem.inventory.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.entity.ArchivableAggregateRoot;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductCategoryId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationMethod;

import java.time.Instant;
import java.util.UUID;

/**
 * Group of products that share inventory valuation defaults (Odoo's product.category).
 * Products inherit the valuation method and accounts from their category unless explicitly
 * overridden on the product itself.
 */
public class ProductCategory extends ArchivableAggregateRoot<ProductCategoryId> {

    private final CompanyId companyId;
    private String name;
    private ProductCategoryId parentId;

    private ValuationMethod valuationMethod;
    private UUID stockValuationAccountId;
    private UUID stockInputAccountId;
    private UUID stockOutputAccountId;
    private UUID cogsAccountId;

    private ProductCategory(Builder b) {
        super.setId(b.id);
        this.companyId = b.companyId;
        this.name = b.name;
        this.parentId = b.parentId;
        this.valuationMethod = b.valuationMethod != null ? b.valuationMethod : ValuationMethod.AVCO;
        this.stockValuationAccountId = b.stockValuationAccountId;
        this.stockInputAccountId = b.stockInputAccountId;
        this.stockOutputAccountId = b.stockOutputAccountId;
        this.cogsAccountId = b.cogsAccountId;
        if (b.archived) {
            super.restoreArchiveState(false, b.archivedAt, b.archivedBy);
        }
    }

    public void validate() {
        if (companyId == null) throw new InventoryDomainException("companyId required");
        if (name == null || name.isBlank()) throw new InventoryDomainException("name required");
        if (valuationMethod == null) throw new InventoryDomainException("valuationMethod required");
    }

    public void rename(String name) {
        if (name == null || name.isBlank()) throw new InventoryDomainException("name required");
        this.name = name;
    }

    public void changeParent(ProductCategoryId parentId) { this.parentId = parentId; }
    public void changeValuationMethod(ValuationMethod method) {
        if (method == null) throw new InventoryDomainException("valuationMethod required");
        this.valuationMethod = method;
    }
    public void changeAccounts(UUID stockValuationAccountId,
                               UUID stockInputAccountId,
                               UUID stockOutputAccountId,
                               UUID cogsAccountId) {
        this.stockValuationAccountId = stockValuationAccountId;
        this.stockInputAccountId = stockInputAccountId;
        this.stockOutputAccountId = stockOutputAccountId;
        this.cogsAccountId = cogsAccountId;
    }

    public CompanyId getCompanyId() { return companyId; }
    public String getName() { return name; }
    public ProductCategoryId getParentId() { return parentId; }
    public ValuationMethod getValuationMethod() { return valuationMethod; }
    public UUID getStockValuationAccountId() { return stockValuationAccountId; }
    public UUID getStockInputAccountId() { return stockInputAccountId; }
    public UUID getStockOutputAccountId() { return stockOutputAccountId; }
    public UUID getCogsAccountId() { return cogsAccountId; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private ProductCategoryId id;
        private CompanyId companyId;
        private String name;
        private ProductCategoryId parentId;
        private ValuationMethod valuationMethod;
        private UUID stockValuationAccountId;
        private UUID stockInputAccountId;
        private UUID stockOutputAccountId;
        private UUID cogsAccountId;
        private boolean archived;
        private Instant archivedAt;
        private String archivedBy;

        public Builder id(ProductCategoryId v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder parentId(ProductCategoryId v) { this.parentId = v; return this; }
        public Builder valuationMethod(ValuationMethod v) { this.valuationMethod = v; return this; }
        public Builder stockValuationAccountId(UUID v) { this.stockValuationAccountId = v; return this; }
        public Builder stockInputAccountId(UUID v) { this.stockInputAccountId = v; return this; }
        public Builder stockOutputAccountId(UUID v) { this.stockOutputAccountId = v; return this; }
        public Builder cogsAccountId(UUID v) { this.cogsAccountId = v; return this; }
        public Builder archived(boolean v) { this.archived = v; return this; }
        public Builder archivedAt(Instant v) { this.archivedAt = v; return this; }
        public Builder archivedBy(String v) { this.archivedBy = v; return this; }
        public ProductCategory build() { return new ProductCategory(this); }
    }
}
