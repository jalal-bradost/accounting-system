package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockQuantResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.ValuationLayerResponse;

import java.math.BigDecimal;
import java.util.List;
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

    /** Stock valuation layers for a product (chronological). */
    List<ValuationLayerResponse> layersByProduct(CompanyId companyId, UUID productId);

    /** Total inventory value for a product (sum of remaining-value across positive layers). */
    BigDecimal valuationOf(CompanyId companyId, UUID productId);
}
