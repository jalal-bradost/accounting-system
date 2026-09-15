package com.jalaldeveloper.accountingsystem.inventory.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductPackagingId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Product-specific packaging option. Stock is always tracked in the product base UOM;
 * {@code qty} is how many base units one package contains.
 */
public class ProductPackaging {

    private ProductPackagingId id;
    private CompanyId companyId;
    private ProductId productId;
    private String name;
    private BigDecimal qty;
    private Money purchasePrice;
    private Money listPrice;
    private String barcode;
    private String sku;
    private boolean active = true;
    private boolean base;
    /** System-managed product that holds stock in pack units (null for base packaging). */
    private ProductId packagedProductId;
    private Instant createdAt;
    private Instant updatedAt;

    public ProductPackaging() {}

    public void validate() {
        if (companyId == null) {
            throw new InventoryDomainException("error.inventory.companyIdRequired", null, "companyId required");
        }
        if (productId == null) {
            throw new InventoryDomainException("error.inventory.productIdRequired", null, "productId required");
        }
        if (name == null || name.isBlank()) {
            throw new InventoryDomainException("error.inventory.packagingNameRequired", null, "packaging name required");
        }
        if (qty == null || qty.signum() <= 0) {
            throw new InventoryDomainException("error.inventory.packagingQtyPositive", null, "packaging qty must be > 0");
        }
        if (purchasePrice != null && purchasePrice.getAmount().signum() < 0) {
            throw new InventoryDomainException("error.inventory.purchasePriceNonNegative", null, "purchasePrice must be >= 0");
        }
        if (listPrice != null && listPrice.getAmount().signum() < 0) {
            throw new InventoryDomainException("error.inventory.listPriceNonNegative", null, "listPrice must be >= 0");
        }
    }

    public static String normalizeBarcode(String barcode) {
        if (barcode == null) return null;
        String trimmed = barcode.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Convert package quantity to base inventory units. */
    public static BigDecimal toBaseQty(BigDecimal packageQty, BigDecimal qtyPerPackage) {
        if (packageQty == null) return BigDecimal.ZERO;
        BigDecimal factor = qtyPerPackage == null || qtyPerPackage.signum() <= 0
                ? BigDecimal.ONE
                : qtyPerPackage;
        return packageQty.multiply(factor);
    }

    /** Convert base inventory units back to package quantity. */
    public static BigDecimal fromBaseQty(BigDecimal baseQty, BigDecimal qtyPerPackage) {
        if (baseQty == null) return BigDecimal.ZERO;
        BigDecimal factor = qtyPerPackage == null || qtyPerPackage.signum() <= 0
                ? BigDecimal.ONE
                : qtyPerPackage;
        return baseQty.divide(factor, 4, java.math.RoundingMode.HALF_UP);
    }

    public static ProductPackaging createBase(CompanyId companyId,
                                              ProductId productId,
                                              String name,
                                              Money purchasePrice,
                                              Money listPrice,
                                              String barcode) {
        ProductPackaging p = new ProductPackaging();
        p.id = new ProductPackagingId(UUID.randomUUID());
        p.companyId = companyId;
        p.productId = productId;
        p.name = name == null || name.isBlank() ? "Unit" : name.trim();
        p.qty = BigDecimal.ONE;
        p.purchasePrice = purchasePrice != null ? purchasePrice : Money.ZERO;
        p.listPrice = listPrice != null ? listPrice : Money.ZERO;
        p.barcode = normalizeBarcode(barcode);
        p.active = true;
        p.base = true;
        Instant now = Instant.now();
        p.createdAt = now;
        p.updatedAt = now;
        p.validate();
        return p;
    }

    public ProductPackagingId getId() { return id; }
    public void setId(ProductPackagingId id) { this.id = id; }
    public CompanyId getCompanyId() { return companyId; }
    public void setCompanyId(CompanyId companyId) { this.companyId = companyId; }
    public ProductId getProductId() { return productId; }
    public void setProductId(ProductId productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getQty() { return qty; }
    public void setQty(BigDecimal qty) { this.qty = qty; }
    public Money getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(Money purchasePrice) { this.purchasePrice = purchasePrice; }
    public Money getListPrice() { return listPrice; }
    public void setListPrice(Money listPrice) { this.listPrice = listPrice; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = normalizeBarcode(barcode); }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku == null || sku.isBlank() ? null : sku.trim(); }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isBase() { return base; }
    public void setBase(boolean base) { this.base = base; }
    public ProductId getPackagedProductId() { return packagedProductId; }
    public void setPackagedProductId(ProductId packagedProductId) { this.packagedProductId = packagedProductId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
