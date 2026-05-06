package com.jalaldeveloper.accountingsystem.contacts.service.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentTermsResponse(UUID id,
                                   UUID companyId,
                                   String name,
                                   int daysNet,
                                   int discountDays,
                                   BigDecimal discountPercent,
                                   boolean active,
                                   Instant archivedAt,
                                   String archivedBy) {}
