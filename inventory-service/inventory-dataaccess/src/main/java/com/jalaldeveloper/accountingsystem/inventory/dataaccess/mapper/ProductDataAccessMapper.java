package com.jalaldeveloper.accountingsystem.inventory.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.ProductEntity;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Product;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductCategoryId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProductDataAccessMapper {

    public Product entityToDomain(ProductEntity e) {
        if (e == null) return null;
        Product.Builder b = Product.builder()
                .id(new ProductId(e.getId()))
                .companyId(new CompanyId(e.getCompanyId()))
                .sku(e.getSku())
                .name(e.getName())
                .barcode(e.getBarcode())
                .description(e.getDescription())
                .productType(e.getProductType())
                .categoryId(e.getCategoryId() != null ? new ProductCategoryId(e.getCategoryId()) : null)
                .uomId(e.getUomId() != null ? new UomId(e.getUomId()) : null)
                .purchaseUomId(e.getPurchaseUomId() != null ? new UomId(e.getPurchaseUomId()) : null)
                .standardCost(new Money(e.getStandardCost() != null ? e.getStandardCost() : BigDecimal.ZERO))
                .listPrice(new Money(e.getListPrice() != null ? e.getListPrice() : BigDecimal.ZERO))
                .purchaseOk(e.isPurchaseOk())
                .saleOk(e.isSaleOk())
                .valuationMethodOverride(e.getValuationMethodOverride())
                .stockValuationAccountIdOverride(e.getStockValuationAccountIdOverride())
                .stockInputAccountIdOverride(e.getStockInputAccountIdOverride())
                .stockOutputAccountIdOverride(e.getStockOutputAccountIdOverride())
                .cogsAccountIdOverride(e.getCogsAccountIdOverride());
        if (!e.isActive()) {
            b.archived(true).archivedAt(e.getArchivedAt()).archivedBy(e.getArchivedBy());
        }
        return b.build();
    }

    public ProductEntity domainToEntity(Product p, ProductEntity existingOrNull) {
        if (p == null) return null;
        ProductEntity e = existingOrNull != null ? existingOrNull : new ProductEntity();
        e.setId(p.getId().getId());
        e.setCompanyId(p.getCompanyId().getId());
        e.setSku(p.getSku());
        e.setName(p.getName());
        e.setBarcode(p.getBarcode());
        e.setDescription(p.getDescription());
        e.setProductType(p.getProductType());
        e.setCategoryId(p.getCategoryId() != null ? p.getCategoryId().getId() : null);
        e.setUomId(p.getUomId() != null ? p.getUomId().getId() : null);
        e.setPurchaseUomId(p.getPurchaseUomId() != null ? p.getPurchaseUomId().getId() : null);
        e.setStandardCost(p.getStandardCost() != null ? p.getStandardCost().getAmount() : BigDecimal.ZERO);
        e.setListPrice(p.getListPrice() != null ? p.getListPrice().getAmount() : BigDecimal.ZERO);
        e.setPurchaseOk(p.isPurchaseOk());
        e.setSaleOk(p.isSaleOk());
        e.setValuationMethodOverride(p.getValuationMethodOverride());
        e.setStockValuationAccountIdOverride(p.getStockValuationAccountIdOverride());
        e.setStockInputAccountIdOverride(p.getStockInputAccountIdOverride());
        e.setStockOutputAccountIdOverride(p.getStockOutputAccountIdOverride());
        e.setCogsAccountIdOverride(p.getCogsAccountIdOverride());
        e.setActive(p.isActive());
        e.setArchivedAt(p.getArchivedAt());
        e.setArchivedBy(p.getArchivedBy());
        return e;
    }
}
