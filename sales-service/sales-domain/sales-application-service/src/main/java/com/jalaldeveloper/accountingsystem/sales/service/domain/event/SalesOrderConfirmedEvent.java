package com.jalaldeveloper.accountingsystem.sales.service.domain.event;

import java.time.Instant;
import java.util.UUID;

public record SalesOrderConfirmedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID companyId,
        UUID salesOrderId
) {}
