package com.jalaldeveloper.accountingsystem.contacts.service.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/** Lightweight projection used by other modules (e.g. accounting) when denormalizing a partner. */
public record PartnerRefResponse(
        UUID id,
        String displayName,
        boolean active,
        @JsonProperty("isCustomer") boolean isCustomer,
        @JsonProperty("isVendor") boolean isVendor) {}
