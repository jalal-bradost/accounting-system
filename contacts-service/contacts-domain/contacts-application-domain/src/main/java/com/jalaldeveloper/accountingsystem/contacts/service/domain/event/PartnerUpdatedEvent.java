package com.jalaldeveloper.accountingsystem.contacts.service.domain.event;

import java.time.Instant;
import java.util.UUID;

public record PartnerUpdatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID companyId,
        UUID partnerId,
        boolean customer,
        boolean vendor
) {}
