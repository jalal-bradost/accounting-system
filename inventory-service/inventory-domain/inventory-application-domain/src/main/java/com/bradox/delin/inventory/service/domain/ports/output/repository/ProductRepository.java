package com.bradox.delin.inventory.service.domain.ports.output.repository;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.inventory.domain.core.entity.Product;
import com.bradox.delin.inventory.domain.core.valueobject.ProductCategoryId;
import com.bradox.delin.inventory.domain.core.valueobject.ProductId;
import com.bradox.delin.inventory.service.domain.dto.ProductImageMeta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(ProductId id);
    Optional<Product> findByIdIncludingArchived(ProductId id);
    Page<Product> search(CompanyId companyId, String query, boolean includeArchived, Pageable pageable);

    /** Active products with {@code saleOk=true} (POS / sales catalog). */
    Page<Product> searchSaleable(CompanyId companyId, String query, Pageable pageable);

    /** Packaging child products created from a parent. */
    java.util.List<Product> findByParentProductId(ProductId parentProductId);

    Optional<ProductImageMeta> findImageMeta(UUID productId);

    Map<UUID, ProductImageMeta> findImageMetaByProductIds(Collection<UUID> productIds);

    void updateImage(UUID productId, String imageUrl, String contentType);

    void clearImage(UUID productId);

    void deleteById(ProductId id);

    /** True when any product references the given category (active or archived). */
    boolean existsByCategory(ProductCategoryId categoryId);

    /**
     * True when the product has stock activity that makes deletion unsafe: any stock move,
     * valuation layer, or stock quant referencing it.
     */
    boolean hasStockActivity(ProductId id);

    Optional<Product> findActiveByCompanyIdAndBarcode(CompanyId companyId, String barcode);

    Optional<Product> findActiveByCompanyIdAndSku(CompanyId companyId, String sku);

    boolean existsByCompanyIdAndBarcodeExcludingId(CompanyId companyId, String barcode, UUID excludeProductId);
}
