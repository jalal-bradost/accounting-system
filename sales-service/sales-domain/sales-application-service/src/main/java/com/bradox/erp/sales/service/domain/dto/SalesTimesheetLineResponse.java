package com.bradox.erp.sales.service.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A sales order line as the Timesheet module sees it (TSH-06). {@code unit} is {@code DAYS} when the line's unit of
 * measure is a day, otherwise {@code HOURS}. {@code eligible} means a service line with the timesheet invoicing policy;
 * {@code closed} means the order is canceled or locked, so no quantity may change.
 */
public record SalesTimesheetLineResponse(UUID lineId, UUID orderId, String orderName, UUID customerPartnerId,
                                         String lineName, String unit, boolean eligible, boolean closed,
                                         BigDecimal qtyOrdered, BigDecimal qtyDelivered, BigDecimal qtyInvoiced) {
}
