package com.bradox.erp.inventory.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.domain.core.entity.StockQuant;
import com.bradox.erp.inventory.domain.core.valueobject.ProductId;
import com.bradox.erp.inventory.domain.core.valueobject.StockLocationId;

import com.bradox.erp.inventory.domain.core.valueobject.WarehouseId;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface StockQuantRepository {

    StockQuant save(StockQuant quant);

    /** Locate the unique quant for {@code (company, product, location)} or empty. */
    Optional<StockQuant> findByProductLocation(CompanyId companyId,
                                               ProductId productId,
                                               StockLocationId locationId);

    /** Total on-hand qty across all internal locations for the product. */
    BigDecimal sumOnHandInternal(CompanyId companyId, ProductId productId);

    Map<UUID, BigDecimal> sumOnHandInternalByProductIds(CompanyId companyId, Collection<UUID> productIds);

    /** On-hand qty in internal locations belonging to the warehouse. */
    BigDecimal sumOnHandByWarehouse(CompanyId companyId, ProductId productId, WarehouseId warehouseId);

    /** Product → on-hand qty for all stocked products in the warehouse. */
    Map<UUID, BigDecimal> sumOnHandGroupedByWarehouse(CompanyId companyId, WarehouseId warehouseId);

    /** All quants for a product (used for valuation reports). */
    List<StockQuant> findByProduct(CompanyId companyId, ProductId productId);

    List<StockQuant> findByLocation(CompanyId companyId, StockLocationId locationId);
}
