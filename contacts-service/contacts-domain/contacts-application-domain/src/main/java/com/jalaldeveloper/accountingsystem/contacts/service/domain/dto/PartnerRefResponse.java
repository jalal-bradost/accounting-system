package com.jalaldeveloper.accountingsystem.contacts.service.domain.dto;

import java.util.UUID;

/** Lightweight projection used by other modules (e.g. accounting) when denormalizing a partner. */
public record PartnerRefResponse(UUID id, String displayName, boolean active,
                                 boolean isCustomer, boolean isVendor) {}
