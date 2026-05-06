package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.ProductCategory;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductCategoryId;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository {
    ProductCategory save(ProductCategory category);
    Optional<ProductCategory> findById(ProductCategoryId id);
    Optional<ProductCategory> findByIdIncludingArchived(ProductCategoryId id);
    List<ProductCategory> findByCompany(CompanyId companyId, boolean includeArchived);
}
