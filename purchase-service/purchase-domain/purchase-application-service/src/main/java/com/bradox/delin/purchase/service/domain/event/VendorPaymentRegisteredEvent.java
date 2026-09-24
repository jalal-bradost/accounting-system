package com.bradox.delin.purchase.service.domain.event;

import java.time.Instant;
import java.util.UUID;

public record VendorPaymentRegisteredEvent(
        UUID eventId,
        Instant occurredAt,
        UUID companyId,
        UUID vendorPaymentId,
        UUID vendorPartnerId,
        UUID vendorBillId
) {}
