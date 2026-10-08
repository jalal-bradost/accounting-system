package com.bradox.erp.repair.service.domain.dto;

import java.math.BigDecimal;
import java.util.List;

/** The lines of an order and their total; {@code total} is null without rep.price.view. */
public record LinesResponse(List<LineResponse> lines, BigDecimal total, boolean pricesVisible, int pendingDiscountApprovals) {
}
