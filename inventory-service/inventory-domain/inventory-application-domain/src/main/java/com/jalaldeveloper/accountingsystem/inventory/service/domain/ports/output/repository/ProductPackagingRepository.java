package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.ProductPackaging;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductPackagingId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductPackagingRepository {
    ProductPackaging save(ProductPackaging packaging);

    Optional<ProductPackaging> findById(ProductPackagingId id);

    List<ProductPackaging> findByProductId(ProductId productId);

    List<ProductPackaging> findByProductIds(Collection<UUID> productIds);

    Optional<ProductPackaging> findBaseByProductId(ProductId productId);

    Optional<ProductPackaging> findActiveByCompanyIdAndBarcode(CompanyId companyId, String barcode);

    boolean existsByProductIdAndNameExcludingId(CompanyId companyId, ProductId productId, String name, UUID excludeId);

    boolean existsByCompanyIdAndBarcodeExcludingId(CompanyId companyId, String barcode, UUID excludeId);

    void deleteById(ProductPackagingId id);
}
