package com.bradox.erp.repair.service.domain.dto;

import com.bradox.erp.repair.domain.core.valueobject.LineType;
import com.bradox.erp.repair.domain.core.valueobject.PricingMode;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A new repair line. A PART with a product takes its description and list price from it; an OPERATION with a
 * labor-guide entry takes its minutes and (flat-rate) price from the guide. Anything typed here overrides.
 */
public record AddLineCommand(@NotNull LineType type, String sectionLabel, UUID productId, String description, BigDecimal qty,
                             BigDecimal unitPrice, BigDecimal discountPercent, UUID laborGuideId, PricingMode pricingMode,
                             Integer standardMinutes, UUID assigneeEmployeeId, UUID vendorPartnerId, BigDecimal vendorCost) {
}
