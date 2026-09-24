package com.bradox.delin.inventory.dataaccess.adapter;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.inventory.dataaccess.entity.ProductCategoryEntity;
import com.bradox.delin.inventory.dataaccess.mapper.ProductCategoryDataAccessMapper;
import com.bradox.delin.inventory.dataaccess.repository.ProductCategoryJpaRepository;
import com.bradox.delin.inventory.domain.core.entity.ProductCategory;
import com.bradox.delin.inventory.domain.core.valueobject.ProductCategoryId;
import com.bradox.delin.inventory.service.domain.ports.output.repository.ProductCategoryRepository;
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

    @Override
    public void deleteById(ProductCategoryId id) {
        jpa.deleteById(id.getId());
    }

    @Override
    public boolean hasChildren(ProductCategoryId id) {
        return jpa.existsByParentId(id.getId());
    }
}
