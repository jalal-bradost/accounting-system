package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockQuant;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockLocationId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StockQuantRepository {

    StockQuant save(StockQuant quant);

    /** Locate the unique quant for {@code (company, product, location)} or empty. */
    Optional<StockQuant> findByProductLocation(CompanyId companyId,
                                               ProductId productId,
                                               StockLocationId locationId);

    /** Total on-hand qty across all internal locations for the product. */
    BigDecimal sumOnHandInternal(CompanyId companyId, ProductId productId);

    /** All quants for a product (used for valuation reports). */
    List<StockQuant> findByProduct(CompanyId companyId, ProductId productId);

    List<StockQuant> findByLocation(CompanyId companyId, StockLocationId locationId);
}
