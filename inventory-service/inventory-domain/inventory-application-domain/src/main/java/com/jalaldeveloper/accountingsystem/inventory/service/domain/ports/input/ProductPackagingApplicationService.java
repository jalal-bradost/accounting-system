package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.ProductPackaging;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductPackagingCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ProductPackagingResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.PackStockCommand;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductPackagingApplicationService {

    List<ProductPackagingResponse> listByProduct(UUID productId);

    List<ProductPackagingResponse> listByProductIds(Collection<UUID> productIds);

    ProductPackagingResponse create(UUID productId, @Valid ProductPackagingCommand command);

    ProductPackagingResponse update(UUID productId, UUID packagingId, @Valid ProductPackagingCommand command);

    void delete(UUID productId, UUID packagingId);

    Optional<ProductPackaging> findActiveByBarcode(CompanyId companyId, String barcode);

    ProductPackagingResponse get(UUID packagingId);

    ProductPackagingResponse pack(UUID productId, UUID packagingId, @Valid PackStockCommand command);

    ProductPackagingResponse unpack(UUID productId, UUID packagingId, @Valid PackStockCommand command);

    /** Ensure a base packaging row exists for the product (idempotent). */
    ProductPackagingResponse ensureBasePackaging(UUID productId);
}
