package com.bradox.erp.repair.service.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A repair line. {@code unitPrice}, {@code discountPercent}, {@code laborRate}, {@code vendorCost} and {@code total} are
 * {@code null} for users without rep.price.view: omitted from the data, not just hidden on screen (D11).
 */
public record LineResponse(UUID id, UUID orderId, String type, String sectionLabel, int sequence, UUID productId, String description,
                           BigDecimal qty, BigDecimal unitPrice, BigDecimal discountPercent, BigDecimal total, String status,
                           boolean needsDiscountApproval, boolean priceMissing, UUID fromFindingId, UUID fromPackageId,
                           UUID laborGuideId, String pricingMode, Integer standardMinutes, BigDecimal laborRate,
                           UUID assigneeEmployeeId, UUID vendorPartnerId, BigDecimal vendorCost, String subletStatus,
                           String disposition, Instant createdAt, String createdBy) {
}
