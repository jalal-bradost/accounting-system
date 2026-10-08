package com.bradox.erp.inventory.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.service.domain.dto.StockQuantResponse;
import com.bradox.erp.inventory.service.domain.dto.ValuationLayerResponse;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface StockValuationApplicationService {

    /** Quants for a single product across all internal locations. */
    List<StockQuantResponse> onHandByProduct(CompanyId companyId, UUID productId);

    /** Quants for a location across all products. */
    List<StockQuantResponse> onHandByLocation(CompanyId companyId, UUID locationId);

    /** Sum of on-hand qty for a product across internal locations. */
    BigDecimal totalOnHand(CompanyId companyId, UUID productId);

    /** On-hand qty for a product in a POS / sales warehouse. */
    BigDecimal totalOnHandForWarehouse(CompanyId companyId, UUID productId, UUID warehouseId);

    /** Product → on-hand qty for every stocked product in the warehouse. */
    Map<UUID, BigDecimal> onHandByWarehouse(CompanyId companyId, UUID warehouseId);

    /** Stock valuation layers for a product (chronological). */
    List<ValuationLayerResponse> layersByProduct(CompanyId companyId, UUID productId);

    /** Total inventory value for a product (sum of remaining-value across positive layers). */
    BigDecimal valuationOf(CompanyId companyId, UUID productId);

    /** Lifetime sold and purchased quantity of a product, net of returns, from validated stock moves. */
    Map<String, BigDecimal> movementTotals(CompanyId companyId, UUID productId);

    /** On-hand qty + valuation for many products in one round-trip. */
    List<Map<String, Object>> bulkValuation(CompanyId companyId, Collection<UUID> productIds);
}

