package com.bradox.erp.inventory.service.domain.dto;

import com.bradox.erp.inventory.domain.core.valueobject.StockHoldOwnerType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class StockHoldDtos {
    private StockHoldDtos() {}

    public record HoldLine(UUID productId, BigDecimal quantity) {}

    public record SyncHoldsCommand(
            StockHoldOwnerType ownerType,
            UUID ownerId,
            UUID warehouseId,
            UUID holderId,
            List<HoldLine> lines) {}

    public record WarehouseAvailabilityRow(
            UUID productId,
            BigDecimal onHand,
            BigDecimal reserved,
            BigDecimal softHeld,
            BigDecimal available) {}

    public record AvailabilityChangedEvent(UUID companyId, UUID warehouseId, List<UUID> productIds) {}
}
