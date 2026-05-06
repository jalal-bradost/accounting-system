package com.jalaldeveloper.accountingsystem.inventory.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity.ProductEntity;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.mapper.ProductDataAccessMapper;
import com.jalaldeveloper.accountingsystem.inventory.dataaccess.repository.ProductJpaRepository;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Product;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository jpa;
    private final ProductDataAccessMapper mapper;

    public ProductRepositoryImpl(ProductJpaRepository jpa, ProductDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Product save(Product product) {
        ProductEntity existing = jpa.findById(product.getId().getId()).orElse(null);
        ProductEntity toSave = mapper.domainToEntity(product, existing);
        return mapper.entityToDomain(jpa.save(toSave));
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return jpa.findById(id.getId())
                .filter(ProductEntity::isActive)
                .map(mapper::entityToDomain);
    }

    @Override
    public Optional<Product> findByIdIncludingArchived(ProductId id) {
        return jpa.findById(id.getId()).map(mapper::entityToDomain);
    }

    @Override
    public Page<Product> search(CompanyId companyId, String query, boolean includeArchived, Pageable pageable) {
        return jpa.search(companyId.getId(), query, includeArchived, pageable)
                .map(mapper::entityToDomain);
    }
}
