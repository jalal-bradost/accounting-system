package com.bradox.erp.repair.service.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RepairOrderCommand(UUID customerPartnerId, UUID productId, Instant scheduledDate, boolean underWarranty,
                                 @Valid List<Part> parts) {

    public record Part(@NotNull UUID productId, @NotNull @Positive BigDecimal qty) {
    }

    public RepairOrderCommand {
        parts = parts == null ? List.of() : parts;
    }
}
