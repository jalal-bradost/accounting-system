package com.bradox.erp.repair.service.domain.dto;

import com.bradox.erp.repair.domain.core.valueobject.RepairStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RepairOrderResponse(UUID id, String reference, UUID customerPartnerId, String customerName,
                                  UUID productId, String productName, Instant scheduledDate, boolean underWarranty,
                                  RepairStatus status, UUID saleOrderId, String saleOrderName, List<Part> parts,
                                  Instant createdAt) {

    public record Part(UUID id, UUID productId, String productName, BigDecimal qty) {
    }
}
