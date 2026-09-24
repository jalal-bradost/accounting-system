package com.bradox.erp.inventory.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.dataaccess.entity.ProductEntity;
import com.bradox.erp.inventory.dataaccess.mapper.ProductDataAccessMapper;
import com.bradox.erp.inventory.dataaccess.repository.ProductJpaRepository;
import com.bradox.erp.inventory.dataaccess.repository.StockMoveJpaRepository;
import com.bradox.erp.inventory.dataaccess.repository.StockQuantJpaRepository;
import com.bradox.erp.inventory.dataaccess.repository.StockValuationLayerJpaRepository;
import com.bradox.erp.inventory.domain.core.entity.Product;
import com.bradox.erp.inventory.domain.core.valueobject.ProductCategoryId;
import com.bradox.erp.inventory.domain.core.valueobject.ProductId;
import com.bradox.erp.inventory.service.domain.dto.ProductImageMeta;
import com.bradox.erp.inventory.service.domain.ports.output.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository jpa;
    private final ProductDataAccessMapper mapper;
    private final StockQuantJpaRepository quantJpa;
    private final StockValuationLayerJpaRepository valuationLayerJpa;
    private final StockMoveJpaRepository moveJpa;

    public ProductRepositoryImpl(ProductJpaRepository jpa,
                                 ProductDataAccessMapper mapper,
                                 StockQuantJpaRepository quantJpa,
                                 StockValuationLayerJpaRepository valuationLayerJpa,
                                 StockMoveJpaRepository moveJpa) {
        this.jpa = jpa;
        this.mapper = mapper;
        this.quantJpa = quantJpa;
        this.valuationLayerJpa = valuationLayerJpa;
        this.moveJpa = moveJpa;
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

    @Override
    public Page<Product> searchSaleable(CompanyId companyId, String query, Pageable pageable) {
        return jpa.searchSaleable(companyId.getId(), query, pageable)
                .map(mapper::entityToDomain);
    }

    @Override
    public List<Product> findByParentProductId(ProductId parentProductId) {
        return jpa.findByParentProductId(parentProductId.getId()).stream()
                .map(mapper::entityToDomain)
                .toList();
    }

    @Override
    public Optional<ProductImageMeta> findImageMeta(UUID productId) {
        return jpa.findById(productId)
                .filter(e -> e.getImageUrl() != null && !e.getImageUrl().isBlank())
                .map(e -> new ProductImageMeta(e.getImageUrl(), e.getImageContentType()));
    }

    @Override
    public Map<UUID, ProductImageMeta> findImageMetaByProductIds(Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) return Map.of();
        Map<UUID, ProductImageMeta> out = new HashMap<>();
        for (ProductEntity e : jpa.findAllById(productIds)) {
            if (e.getImageUrl() != null && !e.getImageUrl().isBlank()) {
                out.put(e.getId(), new ProductImageMeta(e.getImageUrl(), e.getImageContentType()));
            }
        }
        return out;
    }

    @Override
    public void updateImage(UUID productId, String imageUrl, String contentType) {
        ProductEntity e = jpa.findById(productId)
                .orElseThrow(() -> new com.bradox.erp.inventory.domain.core.exception.InventoryDomainException(
                        "Product not found: " + productId));
        e.setImageUrl(imageUrl);
        e.setImageContentType(contentType);
        jpa.save(e);
    }

    @Override
    public void clearImage(UUID productId) {
        ProductEntity e = jpa.findById(productId)
                .orElseThrow(() -> new com.bradox.erp.inventory.domain.core.exception.InventoryDomainException(
                        "Product not found: " + productId));
        e.setImageUrl(null);
        e.setImageContentType(null);
        jpa.save(e);
    }

    @Override
    public void deleteById(ProductId id) {
        jpa.deleteById(id.getId());
    }

    @Override
    public boolean existsByCategory(ProductCategoryId categoryId) {
        return jpa.existsByCategoryId(categoryId.getId());
    }

    @Override
    public boolean hasStockActivity(ProductId id) {
        UUID productId = id.getId();
        return moveJpa.existsByProductId(productId)
                || valuationLayerJpa.existsByProductId(productId)
                || quantJpa.existsByProductId(productId);
    }

    @Override
    public Optional<Product> findActiveByCompanyIdAndBarcode(CompanyId companyId, String barcode) {
        if (barcode == null || barcode.isBlank()) return Optional.empty();
        return jpa.findActiveByCompanyIdAndBarcode(companyId.getId(), barcode.trim())
                .map(mapper::entityToDomain);
    }

    @Override
    public Optional<Product> findActiveByCompanyIdAndSku(CompanyId companyId, String sku) {
        if (sku == null || sku.isBlank()) return Optional.empty();
        return jpa.findActiveByCompanyIdAndSku(companyId.getId(), sku.trim())
                .map(mapper::entityToDomain);
    }

    @Override
    public boolean existsByCompanyIdAndBarcodeExcludingId(CompanyId companyId, String barcode, UUID excludeProductId) {
        if (barcode == null || barcode.isBlank()) return false;
        return jpa.existsByCompanyIdAndBarcodeExcludingId(companyId.getId(), barcode.trim(), excludeProductId);
    }
}
