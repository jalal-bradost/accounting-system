package com.bradox.erp.inventory.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.domain.core.valueobject.StockHoldOwnerType;
import com.bradox.erp.inventory.domain.core.valueobject.WarehouseId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface StockHoldRepository {

    record HoldRow(
            UUID id,
            UUID companyId,
            UUID warehouseId,
            UUID productId,
            BigDecimal quantity,
            UUID holderId,
            StockHoldOwnerType ownerType,
            UUID ownerId,
            Instant expiresAt,
            Instant createdAt,
            Instant updatedAt,
            long version) {}

    List<HoldRow> findByOwner(CompanyId companyId, StockHoldOwnerType ownerType, UUID ownerId);

    Optional<HoldRow> findByOwnerAndProduct(CompanyId companyId,
                                            StockHoldOwnerType ownerType,
                                            UUID ownerId,
                                            UUID productId);

    HoldRow save(HoldRow row);

    void deleteByOwner(CompanyId companyId, StockHoldOwnerType ownerType, UUID ownerId);

    void delete(HoldRow row);

    BigDecimal sumActiveExcludingOwner(CompanyId companyId,
                                       WarehouseId warehouseId,
                                       UUID productId,
                                       Instant now,
                                       StockHoldOwnerType excludeOwnerType,
                                       UUID excludeOwnerId);

    BigDecimal sumActive(CompanyId companyId, WarehouseId warehouseId, UUID productId, Instant now);

    BigDecimal sumActiveExceptSalesOrder(CompanyId companyId,
                                         WarehouseId warehouseId,
                                         UUID productId,
                                         Instant now,
                                         UUID salesOrderId);

    Map<UUID, BigDecimal> sumActiveGroupedByProduct(CompanyId companyId,
                                                    WarehouseId warehouseId,
                                                    Instant now,
                                                    StockHoldOwnerType excludeOwnerType,
                                                    UUID excludeOwnerId);

    /** Ensure lock row exists and acquire PESSIMISTIC_WRITE. */
    void acquireProductLock(CompanyId companyId, WarehouseId warehouseId, UUID productId);

    /** Lock warehouse quants for the given products (PESSIMISTIC_WRITE). */
    void lockWarehouseQuants(CompanyId companyId, WarehouseId warehouseId, Collection<UUID> productIds);

    BigDecimal sumFreeByWarehouse(CompanyId companyId, WarehouseId warehouseId, UUID productId);

    /** productId → [onHand, reserved] */
    Map<UUID, BigDecimal[]> sumOnHandAndReservedByWarehouse(CompanyId companyId, WarehouseId warehouseId);

    int deleteExpired(Instant now);
}
