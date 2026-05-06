package com.jalaldeveloper.accountingsystem.inventory.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.ProductCategoryEntity;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.ProductCategory;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductCategoryId;
import org.springframework.stereotype.Component;

@Component
public class ProductCategoryDataAccessMapper {

    public ProductCategory entityToDomain(ProductCategoryEntity e) {
        if (e == null) return null;
        ProductCategory.Builder b = ProductCategory.builder()
                .id(new ProductCategoryId(e.getId()))
                .companyId(new CompanyId(e.getCompanyId()))
                .name(e.getName())
                .parentId(e.getParentId() != null ? new ProductCategoryId(e.getParentId()) : null)
                .valuationMethod(e.getValuationMethod())
                .stockValuationAccountId(e.getStockValuationAccountId())
                .stockInputAccountId(e.getStockInputAccountId())
                .stockOutputAccountId(e.getStockOutputAccountId())
                .cogsAccountId(e.getCogsAccountId());
        if (!e.isActive()) {
            b.archived(true).archivedAt(e.getArchivedAt()).archivedBy(e.getArchivedBy());
        }
        return b.build();
    }

    public ProductCategoryEntity domainToEntity(ProductCategory c, ProductCategoryEntity existingOrNull) {
        if (c == null) return null;
        ProductCategoryEntity e = existingOrNull != null ? existingOrNull : new ProductCategoryEntity();
        e.setId(c.getId().getId());
        e.setCompanyId(c.getCompanyId().getId());
        e.setName(c.getName());
        e.setParentId(c.getParentId() != null ? c.getParentId().getId() : null);
        e.setValuationMethod(c.getValuationMethod());
        e.setStockValuationAccountId(c.getStockValuationAccountId());
        e.setStockInputAccountId(c.getStockInputAccountId());
        e.setStockOutputAccountId(c.getStockOutputAccountId());
        e.setCogsAccountId(c.getCogsAccountId());
        e.setActive(c.isActive());
        e.setArchivedAt(c.getArchivedAt());
        e.setArchivedBy(c.getArchivedBy());
        return e;
    }
}
