package com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationMethod;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditTrack;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditableModel;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditingEntityListener;
import com.jalaldeveloper.accountingsystem.platform.dataaccess.entity.ArchivableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "inv_product", indexes = {
        @Index(name = "ix_inv_product_company", columnList = "company_id,active"),
        @Index(name = "ix_inv_product_company_sku", columnList = "company_id,sku"),
        @Index(name = "ix_inv_product_company_name", columnList = "company_id,name")
})
@EntityListeners(AuditingEntityListener.class)
@AuditableModel("inventory.product")
public class ProductEntity extends ArchivableEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 100)
    @AuditTrack
    private String sku;

    @Column(nullable = false, length = 255)
    @AuditTrack
    private String name;

    @Column(length = 100)
    private String barcode;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 20)
    @AuditTrack(name = "productType")
    private ProductType productType;

    @Column(name = "category_id", nullable = false)
    @AuditTrack(name = "categoryId")
    private UUID categoryId;

    @Column(name = "uom_id", nullable = false)
    private UUID uomId;

    @Column(name = "purchase_uom_id")
    private UUID purchaseUomId;

    @Column(name = "standard_cost", precision = 19, scale = 4, nullable = false)
    @AuditTrack(name = "standardCost")
    private BigDecimal standardCost;

    @Column(name = "list_price", precision = 19, scale = 4, nullable = false)
    private BigDecimal listPrice;

    @Column(name = "purchase_ok", nullable = false)
    private boolean purchaseOk;

    @Column(name = "sale_ok", nullable = false)
    private boolean saleOk;

    @Enumerated(EnumType.STRING)
    @Column(name = "valuation_method_override", length = 20)
    @AuditTrack(name = "valuationMethodOverride")
    private ValuationMethod valuationMethodOverride;

    @Column(name = "stock_valuation_account_id_override")
    private UUID stockValuationAccountIdOverride;

    @Column(name = "stock_input_account_id_override")
    private UUID stockInputAccountIdOverride;

    @Column(name = "stock_output_account_id_override")
    private UUID stockOutputAccountIdOverride;

    @Column(name = "cogs_account_id_override")
    private UUID cogsAccountIdOverride;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "image_content_type", length = 100)
    private String imageContentType;

    public ProductEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public String getSku() { return sku; }
    public void setSku(String v) { this.sku = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String v) { this.barcode = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType v) { this.productType = v; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID v) { this.categoryId = v; }
    public UUID getUomId() { return uomId; }
    public void setUomId(UUID v) { this.uomId = v; }
    public UUID getPurchaseUomId() { return purchaseUomId; }
    public void setPurchaseUomId(UUID v) { this.purchaseUomId = v; }
    public BigDecimal getStandardCost() { return standardCost; }
    public void setStandardCost(BigDecimal v) { this.standardCost = v; }
    public BigDecimal getListPrice() { return listPrice; }
    public void setListPrice(BigDecimal v) { this.listPrice = v; }
    public boolean isPurchaseOk() { return purchaseOk; }
    public void setPurchaseOk(boolean v) { this.purchaseOk = v; }
    public boolean isSaleOk() { return saleOk; }
    public void setSaleOk(boolean v) { this.saleOk = v; }
    public ValuationMethod getValuationMethodOverride() { return valuationMethodOverride; }
    public void setValuationMethodOverride(ValuationMethod v) { this.valuationMethodOverride = v; }
    public UUID getStockValuationAccountIdOverride() { return stockValuationAccountIdOverride; }
    public void setStockValuationAccountIdOverride(UUID v) { this.stockValuationAccountIdOverride = v; }
    public UUID getStockInputAccountIdOverride() { return stockInputAccountIdOverride; }
    public void setStockInputAccountIdOverride(UUID v) { this.stockInputAccountIdOverride = v; }
    public UUID getStockOutputAccountIdOverride() { return stockOutputAccountIdOverride; }
    public void setStockOutputAccountIdOverride(UUID v) { this.stockOutputAccountIdOverride = v; }
    public UUID getCogsAccountIdOverride() { return cogsAccountIdOverride; }
    public void setCogsAccountIdOverride(UUID v) { this.cogsAccountIdOverride = v; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String v) { this.imageUrl = v; }
    public String getImageContentType() { return imageContentType; }
    public void setImageContentType(String v) { this.imageContentType = v; }
}
