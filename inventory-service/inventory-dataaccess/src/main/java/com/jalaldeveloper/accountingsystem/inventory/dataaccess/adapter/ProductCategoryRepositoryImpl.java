package com.jalaldeveloper.accountingsystem.inventory.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.ProductCategoryEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.mapper.ProductCategoryDataAccessMapper;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.ProductCategoryJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.ProductCategory;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductCategoryId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.ProductCategoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ProductCategoryRepositoryImpl implements ProductCategoryRepository {

    private final ProductCategoryJpaRepository jpa;
    private final ProductCategoryDataAccessMapper mapper;

    public ProductCategoryRepositoryImpl(ProductCategoryJpaRepository jpa,
                                         ProductCategoryDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public ProductCategory save(ProductCategory category) {
        ProductCategoryEntity existing = jpa.findById(category.getId().getId()).orElse(null);
        ProductCategoryEntity toSave = mapper.domainToEntity(category, existing);
        return mapper.entityToDomain(jpa.save(toSave));
    }

    @Override
    public Optional<ProductCategory> findById(ProductCategoryId id) {
        return jpa.findById(id.getId())
                .filter(ProductCategoryEntity::isActive)
                .map(mapper::entityToDomain);
    }

    @Override
    public Optional<ProductCategory> findByIdIncludingArchived(ProductCategoryId id) {
        return jpa.findById(id.getId()).map(mapper::entityToDomain);
    }

    @Override
    public List<ProductCategory> findByCompany(CompanyId companyId, boolean includeArchived) {
        return jpa.findByCompany(companyId.getId(), includeArchived).stream()
                .map(mapper::entityToDomain)
                .toList();
    }
}
