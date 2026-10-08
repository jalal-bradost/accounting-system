package com.bradox.erp.timesheet.service.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleLineResponse(UUID lineId, UUID orderId, String orderName, UUID customerPartnerId, String lineName,
                               String label, String unit, boolean eligible, boolean closed, BigDecimal qtyDelivered,
                               BigDecimal qtyInvoiced) {
}
