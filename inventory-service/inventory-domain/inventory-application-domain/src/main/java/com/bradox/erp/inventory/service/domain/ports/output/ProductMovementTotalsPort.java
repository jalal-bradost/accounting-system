package com.bradox.erp.inventory.service.domain.ports.output;

import java.math.BigDecimal;
import java.util.UUID;

/** Lifetime quantities that actually moved for a product, from validated stock moves. */
public interface ProductMovementTotalsPort {

    /**
     * Sold: delivered to customers on sales orders minus customer returns. Gift lines are not sales
     * and are left out.
     */
    BigDecimal soldQuantity(UUID companyId, UUID productId);

    /** Purchased: received from vendors on purchase orders minus returns to vendors. */
    BigDecimal purchasedQuantity(UUID companyId, UUID productId);
}
