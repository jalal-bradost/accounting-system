package com.jalaldeveloper.accountingsystem.inventory.service.domain.event;

import java.time.Instant;
import java.util.UUID;

public record StockPickingValidatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID companyId,
        UUID pickingId,
        UUID purchaseOrderId,
        UUID salesOrderId
) {}
