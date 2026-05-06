package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockValuationLayer;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationLayerId;

import java.util.List;
import java.util.Optional;

public interface StockValuationLayerRepository {

    StockValuationLayer save(StockValuationLayer layer);

    /** Persist multiple layers / consumed-layer updates atomically. */
    List<StockValuationLayer> saveAll(List<StockValuationLayer> layers);

    Optional<StockValuationLayer> findById(ValuationLayerId id);

    /** All FIFO candidates (positive layers with remaining qty &gt; 0), oldest first. */
    List<StockValuationLayer> findFifoCandidates(CompanyId companyId, ProductId productId);

    /** Sum of remaining-value across all positive layers (per product). */
    Money sumOnHandValue(CompanyId companyId, ProductId productId);

    /** All layers for a product (chronological). */
    List<StockValuationLayer> findByProduct(CompanyId companyId, ProductId productId);
}
