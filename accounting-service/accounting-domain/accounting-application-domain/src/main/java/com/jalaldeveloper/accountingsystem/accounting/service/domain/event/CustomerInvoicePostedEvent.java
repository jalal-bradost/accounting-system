package com.jalaldeveloper.accountingsystem.accounting.service.domain.event;

import java.time.Instant;
import java.util.UUID;

public record CustomerInvoicePostedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID companyId,
        UUID invoiceId,
        UUID customerPartnerId
) {}
