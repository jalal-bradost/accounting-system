package com.bradox.erp.inventory.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.dataaccess.entity.ProductPackagingEntity;
import com.bradox.erp.inventory.dataaccess.mapper.ProductPackagingDataAccessMapper;
import com.bradox.erp.inventory.dataaccess.repository.ProductPackagingJpaRepository;
import com.bradox.erp.inventory.domain.core.entity.ProductPackaging;
import com.bradox.erp.inventory.domain.core.valueobject.ProductId;
import com.bradox.erp.inventory.domain.core.valueobject.ProductPackagingId;
import com.bradox.erp.inventory.service.domain.ports.output.repository.ProductPackagingRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProductPackagingRepositoryImpl implements ProductPackagingRepository {

    private final ProductPackagingJpaRepository jpa;
    private final ProductPackagingDataAccessMapper mapper;

    public ProductPackagingRepositoryImpl(ProductPackagingJpaRepository jpa,
                                          ProductPackagingDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public ProductPackaging save(ProductPackaging packaging) {
        ProductPackagingEntity entity = jpa.findById(packaging.getId().getId())
                .orElseGet(ProductPackagingEntity::new);
        mapper.updateEntity(entity, packaging);
        return mapper.entityToDomain(jpa.save(entity));
    }

    @Override
    public Optional<ProductPackaging> findById(ProductPackagingId id) {
        return jpa.findById(id.getId()).map(mapper::entityToDomain);
    }

    @Override
    public List<ProductPackaging> findByProductId(ProductId productId) {
        return jpa.findByProductIdOrderByBaseDescNameAsc(productId.getId()).stream()
                .map(mapper::entityToDomain)
                .toList();
    }

    @Override
    public List<ProductPackaging> findByProductIds(Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) return List.of();
        return jpa.findByProductIdInOrderByProductIdAscBaseDescNameAsc(productIds).stream()
                .map(mapper::entityToDomain)
                .toList();
    }

    @Override
    public Optional<ProductPackaging> findBaseByProductId(ProductId productId) {
        return jpa.findByProductIdAndBaseTrue(productId.getId()).map(mapper::entityToDomain);
    }

    @Override
    public Optional<ProductPackaging> findActiveByCompanyIdAndBarcode(CompanyId companyId, String barcode) {
        if (barcode == null || barcode.isBlank()) return Optional.empty();
        return jpa.findActiveByCompanyIdAndBarcode(companyId.getId(), barcode.trim())
                .map(mapper::entityToDomain);
    }

    @Override
    public boolean existsByProductIdAndNameExcludingId(CompanyId companyId, ProductId productId, String name, UUID excludeId) {
        return jpa.existsByProductIdAndNameExcludingId(companyId.getId(), productId.getId(), name, excludeId);
    }

    @Override
    public boolean existsByCompanyIdAndBarcodeExcludingId(CompanyId companyId, String barcode, UUID excludeId) {
        if (barcode == null || barcode.isBlank()) return false;
        return jpa.existsByCompanyIdAndBarcodeExcludingId(companyId.getId(), barcode.trim(), excludeId);
    }

    @Override
    public void deleteById(ProductPackagingId id) {
        jpa.deleteById(id.getId());
    }
}
