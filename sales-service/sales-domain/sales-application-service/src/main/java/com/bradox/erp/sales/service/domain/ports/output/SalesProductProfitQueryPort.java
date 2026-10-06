package com.bradox.erp.sales.service.domain.ports.output;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SalesProductProfitQueryPort {

    /** Confirmed non-gift product sales in the order/confirm date range (qty/revenue net of returns through {@code to}). */
    record ConfirmedProductFact(
            UUID productId,
            String productName,
            long orderCount,
            BigDecimal qtyOrdered,
            BigDecimal revenue) {}

    /** Delivered non-gift product sales valued from stock moves in the delivery date range. */
    record DeliveredProductFact(
            UUID productId,
            String productName,
            BigDecimal qtyDelivered,
            BigDecimal revenue,
            BigDecimal cost) {}

    List<ConfirmedProductFact> confirmedProductFacts(UUID companyId, LocalDate from, LocalDate to);

    List<DeliveredProductFact> deliveredProductFacts(UUID companyId, LocalDate from, LocalDate to);

    /**
     * Current valuation unit cost per product: {@code SVL value / on-hand qty} when on-hand &gt; 0,
     * otherwise {@code standard_cost}. Non-stockable products map to zero.
     */
    Map<UUID, BigDecimal> currentValuationUnitCosts(UUID companyId, Collection<UUID> productIds);
}
