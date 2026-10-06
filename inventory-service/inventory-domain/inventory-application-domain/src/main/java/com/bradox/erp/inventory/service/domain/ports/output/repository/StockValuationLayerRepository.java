package com.bradox.erp.inventory.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.Money;
import com.bradox.erp.inventory.domain.core.entity.StockValuationLayer;
import com.bradox.erp.inventory.domain.core.valueobject.ProductId;
import com.bradox.erp.inventory.domain.core.valueobject.ValuationLayerId;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface StockValuationLayerRepository {

    StockValuationLayer save(StockValuationLayer layer);

    /** Persist multiple layers / consumed-layer updates atomically. */
    List<StockValuationLayer> saveAll(List<StockValuationLayer> layers);

    Optional<StockValuationLayer> findById(ValuationLayerId id);

    /** All FIFO candidates (positive layers with remaining qty &gt; 0), oldest first. */
    List<StockValuationLayer> findFifoCandidates(CompanyId companyId, ProductId productId);

    /** Sum of remaining-value across all positive layers (per product). */
    Money sumOnHandValue(CompanyId companyId, ProductId productId);

    Map<UUID, BigDecimal> sumOnHandValueByProductIds(CompanyId companyId, Collection<UUID> productIds);

    /** All layers for a product (chronological). */
    List<StockValuationLayer> findByProduct(CompanyId companyId, ProductId productId);
}
