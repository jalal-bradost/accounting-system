package com.jalaldeveloper.accountingsystem.inventory.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.entity.ArchivableAggregateRoot;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductCategoryId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A purchasable / sellable / stockable item. {@link ProductType#STOCKABLE} products are the
 * only ones that participate in stock moves and inventory valuation; {@link
 * ProductType#CONSUMABLE} can be moved on pickings without on-hand tracking; {@link
 * ProductType#SERVICE} is never moved physically.
 *
 * <p>Valuation method, accounts and category-driven defaults can be overridden at the
 * product level. When a field is {@code null}, callers (application services) must fall
 * back to {@link ProductCategory}.
 */
public class Product extends ArchivableAggregateRoot<ProductId> {

    private final CompanyId companyId;
    private String sku;
    private String name;
    private String barcode;
    private String description;
    private ProductType productType;
    private ProductCategoryId categoryId;
    private UomId uomId;
    private UomId purchaseUomId;
    private Money standardCost;
    private Money listPrice;
    private boolean purchaseOk;
    private boolean saleOk;
    private ValuationMethod valuationMethodOverride;
    private UUID stockValuationAccountIdOverride;
    private UUID stockInputAccountIdOverride;
    private UUID stockOutputAccountIdOverride;
    private UUID cogsAccountIdOverride;

    private Product(Builder b) {
        super.setId(b.id);
        this.companyId = b.companyId;
        this.sku = b.sku;
        this.name = b.name;
        this.barcode = b.barcode;
        this.description = b.description;
        this.productType = b.productType != null ? b.productType : ProductType.STOCKABLE;
        this.categoryId = b.categoryId;
        this.uomId = b.uomId;
        this.purchaseUomId = b.purchaseUomId != null ? b.purchaseUomId : b.uomId;
        this.standardCost = b.standardCost != null ? b.standardCost : Money.ZERO;
        this.listPrice = b.listPrice != null ? b.listPrice : Money.ZERO;
        this.purchaseOk = b.purchaseOk;
        this.saleOk = b.saleOk;
        this.valuationMethodOverride = b.valuationMethodOverride;
        this.stockValuationAccountIdOverride = b.stockValuationAccountIdOverride;
        this.stockInputAccountIdOverride = b.stockInputAccountIdOverride;
        this.stockOutputAccountIdOverride = b.stockOutputAccountIdOverride;
        this.cogsAccountIdOverride = b.cogsAccountIdOverride;
        if (b.archived) {
            super.restoreArchiveState(false, b.archivedAt, b.archivedBy);
        }
    }

    public void validate() {
        if (companyId == null) throw new InventoryDomainException("companyId required");
        if (sku == null || sku.isBlank()) throw new InventoryDomainException("sku required");
        if (name == null || name.isBlank()) throw new InventoryDomainException("name required");
        if (productType == null) throw new InventoryDomainException("productType required");
        if (uomId == null) throw new InventoryDomainException("uomId required");
        if (categoryId == null) throw new InventoryDomainException("categoryId required");
        if (standardCost != null && standardCost.getAmount().signum() < 0) {
            throw new InventoryDomainException("standardCost must be >= 0");
        }
        if (listPrice != null && listPrice.getAmount().signum() < 0) {
            throw new InventoryDomainException("listPrice must be >= 0");
        }
    }

    /** Whether this product participates in stock moves and on-hand tracking. */
    public boolean tracksStock() {
        return productType == ProductType.STOCKABLE;
    }

    /** Whether this product produces accounting valuation entries (subset of {@link #tracksStock()}). */
    public boolean isValued() {
        return productType == ProductType.STOCKABLE;
    }

    public ValuationMethod resolveValuationMethod(ProductCategory fallback) {
        if (valuationMethodOverride != null) return valuationMethodOverride;
        if (fallback == null) {
            throw new InventoryDomainException(
                    "Cannot resolve valuation method: no override and no category provided for product " + sku);
        }
        return fallback.getValuationMethod();
    }

    public UUID resolveStockValuationAccountId(ProductCategory fallback) {
        return stockValuationAccountIdOverride != null
                ? stockValuationAccountIdOverride
                : (fallback != null ? fallback.getStockValuationAccountId() : null);
    }

    public UUID resolveStockInputAccountId(ProductCategory fallback) {
        return stockInputAccountIdOverride != null
                ? stockInputAccountIdOverride
                : (fallback != null ? fallback.getStockInputAccountId() : null);
    }

    public UUID resolveStockOutputAccountId(ProductCategory fallback) {
        return stockOutputAccountIdOverride != null
                ? stockOutputAccountIdOverride
                : (fallback != null ? fallback.getStockOutputAccountId() : null);
    }

    public UUID resolveCogsAccountId(ProductCategory fallback) {
        return cogsAccountIdOverride != null
                ? cogsAccountIdOverride
                : (fallback != null ? fallback.getCogsAccountId() : null);
    }

    public void rename(String name) {
        if (name == null || name.isBlank()) throw new InventoryDomainException("name required");
        this.name = name;
    }

    public void changeSku(String sku) {
        if (sku == null || sku.isBlank()) throw new InventoryDomainException("sku required");
        this.sku = sku;
    }

    public void changeDescription(String description) { this.description = description; }
    public void changeBarcode(String barcode) { this.barcode = barcode; }
    public void changeCategory(ProductCategoryId categoryId) {
        if (categoryId == null) throw new InventoryDomainException("categoryId required");
        this.categoryId = categoryId;
    }
    public void changeProductType(ProductType type) {
        if (type == null) throw new InventoryDomainException("productType required");
        this.productType = type;
    }
    public void changeUom(UomId uomId) {
        if (uomId == null) throw new InventoryDomainException("uomId required");
        this.uomId = uomId;
    }
    public void changePurchaseUom(UomId purchaseUomId) { this.purchaseUomId = purchaseUomId; }
    public void changeStandardCost(Money cost) {
        if (cost == null || cost.getAmount().signum() < 0) {
            throw new InventoryDomainException("standardCost must be >= 0");
        }
        this.standardCost = cost;
    }
    public void changeListPrice(Money price) {
        if (price == null || price.getAmount().signum() < 0) {
            throw new InventoryDomainException("listPrice must be >= 0");
        }
        this.listPrice = price;
    }
    public void changePurchaseOk(boolean v) { this.purchaseOk = v; }
    public void changeSaleOk(boolean v) { this.saleOk = v; }
    public void changeValuationMethodOverride(ValuationMethod v) { this.valuationMethodOverride = v; }

    public CompanyId getCompanyId() { return companyId; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getBarcode() { return barcode; }
    public String getDescription() { return description; }
    public ProductType getProductType() { return productType; }
    public ProductCategoryId getCategoryId() { return categoryId; }
    public UomId getUomId() { return uomId; }
    public UomId getPurchaseUomId() { return purchaseUomId; }
    public Money getStandardCost() { return standardCost; }
    public Money getListPrice() { return listPrice; }
    public boolean isPurchaseOk() { return purchaseOk; }
    public boolean isSaleOk() { return saleOk; }
    public ValuationMethod getValuationMethodOverride() { return valuationMethodOverride; }
    public UUID getStockValuationAccountIdOverride() { return stockValuationAccountIdOverride; }
    public UUID getStockInputAccountIdOverride() { return stockInputAccountIdOverride; }
    public UUID getStockOutputAccountIdOverride() { return stockOutputAccountIdOverride; }
    public UUID getCogsAccountIdOverride() { return cogsAccountIdOverride; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private ProductId id;
        private CompanyId companyId;
        private String sku;
        private String name;
        private String barcode;
        private String description;
        private ProductType productType;
        private ProductCategoryId categoryId;
        private UomId uomId;
        private UomId purchaseUomId;
        private Money standardCost;
        private Money listPrice;
        private boolean purchaseOk = true;
        private boolean saleOk = true;
        private ValuationMethod valuationMethodOverride;
        private UUID stockValuationAccountIdOverride;
        private UUID stockInputAccountIdOverride;
        private UUID stockOutputAccountIdOverride;
        private UUID cogsAccountIdOverride;
        private boolean archived;
        private Instant archivedAt;
        private String archivedBy;

        public Builder id(ProductId v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder sku(String v) { this.sku = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder barcode(String v) { this.barcode = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder productType(ProductType v) { this.productType = v; return this; }
        public Builder categoryId(ProductCategoryId v) { this.categoryId = v; return this; }
        public Builder uomId(UomId v) { this.uomId = v; return this; }
        public Builder purchaseUomId(UomId v) { this.purchaseUomId = v; return this; }
        public Builder standardCost(Money v) { this.standardCost = v; return this; }
        public Builder listPrice(Money v) { this.listPrice = v; return this; }
        public Builder purchaseOk(boolean v) { this.purchaseOk = v; return this; }
        public Builder saleOk(boolean v) { this.saleOk = v; return this; }
        public Builder valuationMethodOverride(ValuationMethod v) { this.valuationMethodOverride = v; return this; }
        public Builder stockValuationAccountIdOverride(UUID v) { this.stockValuationAccountIdOverride = v; return this; }
        public Builder stockInputAccountIdOverride(UUID v) { this.stockInputAccountIdOverride = v; return this; }
        public Builder stockOutputAccountIdOverride(UUID v) { this.stockOutputAccountIdOverride = v; return this; }
        public Builder cogsAccountIdOverride(UUID v) { this.cogsAccountIdOverride = v; return this; }
        public Builder archived(boolean v) { this.archived = v; return this; }
        public Builder archivedAt(Instant v) { this.archivedAt = v; return this; }
        public Builder archivedBy(String v) { this.archivedBy = v; return this; }
        public Product build() { return new Product(this); }
    }
}
