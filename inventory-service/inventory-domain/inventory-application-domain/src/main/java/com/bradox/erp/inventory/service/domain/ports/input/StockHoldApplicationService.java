package com.bradox.erp.inventory.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.domain.core.valueobject.StockHoldOwnerType;
import com.bradox.erp.inventory.service.domain.dto.StockHoldDtos.SyncHoldsCommand;
import com.bradox.erp.inventory.service.domain.dto.StockHoldDtos.WarehouseAvailabilityRow;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

public interface StockHoldApplicationService {

    Duration CART_TTL = Duration.ofMinutes(15);
    Duration DRAFT_TTL = Duration.ofHours(24);
    Duration SALES_ORDER_TTL = Duration.ofHours(24);

    /** Replace-all soft holds for an owner. Fails atomically if any product lacks stock. */
    void syncHolds(CompanyId companyId, SyncHoldsCommand command);

    /** Transfer holds from one owner to another and refresh TTL. */
    void rebindOwner(CompanyId companyId,
                     StockHoldOwnerType fromType, UUID fromId,
                     StockHoldOwnerType toType, UUID toId,
                     Duration newTtl);

    void releaseOwner(CompanyId companyId, StockHoldOwnerType ownerType, UUID ownerId);

    /** Extend CART hold expiry (heartbeat). */
    void heartbeat(CompanyId companyId, StockHoldOwnerType ownerType, UUID ownerId, Duration ttl);

    List<WarehouseAvailabilityRow> availableByWarehouse(CompanyId companyId,
                                                        UUID warehouseId,
                                                        StockHoldOwnerType excludeOwnerType,
                                                        UUID excludeOwnerId);

    /** Free qty for one product at warehouse, optionally excluding one owner's holds. */
    BigDecimal availableQuantity(CompanyId companyId,
                                 UUID warehouseId,
                                 UUID productId,
                                 StockHoldOwnerType excludeOwnerType,
                                 UUID excludeOwnerId);

    /** Soft holds that still block assign capacity (excluding a sales order's own holds). */
    BigDecimal softHeldExceptSalesOrder(CompanyId companyId,
                                        UUID warehouseId,
                                        UUID productId,
                                        UUID salesOrderId);

    /** Reduce / delete SO soft holds after converting to hard reserve. */
    void reduceSalesOrderHold(CompanyId companyId, UUID salesOrderId, UUID productId, BigDecimal qty);

    int purgeExpired();
}
