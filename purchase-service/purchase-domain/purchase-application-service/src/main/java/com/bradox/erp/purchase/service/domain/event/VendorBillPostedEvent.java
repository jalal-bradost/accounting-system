package com.bradox.erp.purchase.service.domain.event;

import java.time.Instant;
import java.util.UUID;

public record VendorBillPostedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID companyId,
        UUID vendorBillId,
        UUID vendorPartnerId
) {}
