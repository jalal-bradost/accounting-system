package com.bradox.erp.inventory.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.domain.core.entity.Product;
import com.bradox.erp.inventory.domain.core.exception.InventoryDomainException;
import com.bradox.erp.inventory.domain.core.valueobject.ProductId;
import com.bradox.erp.inventory.domain.core.valueobject.ProductType;
import com.bradox.erp.inventory.domain.core.valueobject.StockHoldOwnerType;
import com.bradox.erp.inventory.domain.core.valueobject.WarehouseId;
import com.bradox.erp.inventory.service.domain.dto.StockHoldDtos.AvailabilityChangedEvent;
import com.bradox.erp.inventory.service.domain.dto.StockHoldDtos.HoldLine;
import com.bradox.erp.inventory.service.domain.dto.StockHoldDtos.SyncHoldsCommand;
import com.bradox.erp.inventory.service.domain.dto.StockHoldDtos.WarehouseAvailabilityRow;
import com.bradox.erp.inventory.service.domain.ports.input.StockHoldApplicationService;
import com.bradox.erp.inventory.service.domain.ports.output.repository.ProductRepository;
import com.bradox.erp.inventory.service.domain.ports.output.repository.StockHoldRepository;
import com.bradox.erp.inventory.service.domain.ports.output.repository.StockHoldRepository.HoldRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class StockHoldApplicationServiceImpl implements StockHoldApplicationService {

    private final StockHoldRepository holdRepository;
    private final ProductRepository productRepository;
    private final StockHoldAvailabilityHub availabilityHub;

    public StockHoldApplicationServiceImpl(StockHoldRepository holdRepository,
                                           ProductRepository productRepository,
                                           StockHoldAvailabilityHub availabilityHub) {
        this.holdRepository = holdRepository;
        this.productRepository = productRepository;
        this.availabilityHub = availabilityHub;
    }

    @Override
    @Transactional
    public void syncHolds(CompanyId companyId, SyncHoldsCommand command) {
        if (command == null || command.ownerType() == null || command.ownerId() == null
                || command.warehouseId() == null || command.holderId() == null) {
            throw new InventoryDomainException("error.inventory.holdInvalidCommand", null, "Invalid stock hold sync command");
        }
        Map<UUID, BigDecimal> demanded = aggregateStockableLines(companyId, command.lines());
        WarehouseId warehouseId = new WarehouseId(command.warehouseId());
        Instant now = Instant.now();
        Duration ttl = ttlFor(command.ownerType());
        Instant expiresAt = now.plus(ttl);

        List<UUID> productIds = new ArrayList<>(demanded.keySet());
        // Also lock products currently held by this owner that may be removed.
        for (HoldRow existing : holdRepository.findByOwner(companyId, command.ownerType(), command.ownerId())) {
            if (!productIds.contains(existing.productId())) {
                productIds.add(existing.productId());
            }
        }

        for (UUID productId : productIds) {
            holdRepository.acquireProductLock(companyId, warehouseId, productId);
        }
        holdRepository.lockWarehouseQuants(companyId, warehouseId, productIds);

        for (Map.Entry<UUID, BigDecimal> e : demanded.entrySet()) {
            UUID productId = e.getKey();
            BigDecimal want = e.getValue();
            BigDecimal free = holdRepository.sumFreeByWarehouse(companyId, warehouseId, productId);
            BigDecimal others = holdRepository.sumActiveExcludingOwner(
                    companyId, warehouseId, productId, now, command.ownerType(), command.ownerId());
            BigDecimal available = free.subtract(others);
            if (want.compareTo(available) > 0) {
                throw new InventoryDomainException(
                        "error.inventory.insufficientStockHold",
                        new Object[]{productId, want, available.max(BigDecimal.ZERO)},
                        "Insufficient available stock to hold (product=" + productId
                                + ", requested=" + want + ", available=" + available.max(BigDecimal.ZERO) + ")");
            }
        }

        Set<UUID> keep = new HashSet<>(demanded.keySet());
        for (HoldRow existing : holdRepository.findByOwner(companyId, command.ownerType(), command.ownerId())) {
            if (!keep.contains(existing.productId())) {
                holdRepository.delete(existing);
            }
        }

        for (Map.Entry<UUID, BigDecimal> e : demanded.entrySet()) {
            HoldRow existing = holdRepository.findByOwnerAndProduct(
                    companyId, command.ownerType(), command.ownerId(), e.getKey()).orElse(null);
            HoldRow next = new HoldRow(
                    existing != null ? existing.id() : UUID.randomUUID(),
                    companyId.getId(),
                    command.warehouseId(),
                    e.getKey(),
                    e.getValue(),
                    command.holderId(),
                    command.ownerType(),
                    command.ownerId(),
                    expiresAt,
                    existing != null ? existing.createdAt() : now,
                    now,
                    existing != null ? existing.version() : 0L);
            holdRepository.save(next);
        }

        publish(companyId.getId(), command.warehouseId(), productIds);
    }

    @Override
    @Transactional
    public void rebindOwner(CompanyId companyId,
                            StockHoldOwnerType fromType, UUID fromId,
                            StockHoldOwnerType toType, UUID toId,
                            Duration newTtl) {
        if (fromType == null || fromId == null || toType == null || toId == null) return;
        List<HoldRow> fromRows = holdRepository.findByOwner(companyId, fromType, fromId);
        if (fromRows.isEmpty()) return;
        Instant now = Instant.now();
        Instant expiresAt = now.plus(newTtl != null ? newTtl : ttlFor(toType));
        UUID warehouseId = fromRows.get(0).warehouseId();
        UUID holderId = fromRows.get(0).holderId();

        Map<UUID, BigDecimal> qtyByProduct = new LinkedHashMap<>();
        for (HoldRow row : fromRows) {
            qtyByProduct.merge(row.productId(), row.quantity(), BigDecimal::add);
        }
        // Release source first so sync for target does not double-count.
        holdRepository.deleteByOwner(companyId, fromType, fromId);

        List<HoldLine> lines = qtyByProduct.entrySet().stream()
                .map(e -> new HoldLine(e.getKey(), e.getValue()))
                .toList();
        // Direct upsert with locks (same as sync, but use provided TTL via owner type default —
        // temporarily save with desired expiry after sync).
        syncHolds(companyId, new SyncHoldsCommand(toType, toId, warehouseId, holderId, lines));
        // Refresh expiry to requested TTL (sync uses owner-type default which matches).
        if (newTtl != null && !newTtl.equals(ttlFor(toType))) {
            Instant customExpiry = now.plus(newTtl);
            for (HoldRow row : holdRepository.findByOwner(companyId, toType, toId)) {
                holdRepository.save(new HoldRow(
                        row.id(), row.companyId(), row.warehouseId(), row.productId(), row.quantity(),
                        row.holderId(), row.ownerType(), row.ownerId(),
                        customExpiry, row.createdAt(), now, row.version()));
            }
        }
        publish(companyId.getId(), warehouseId, new ArrayList<>(qtyByProduct.keySet()));
    }

    @Override
    @Transactional
    public void releaseOwner(CompanyId companyId, StockHoldOwnerType ownerType, UUID ownerId) {
        List<HoldRow> rows = holdRepository.findByOwner(companyId, ownerType, ownerId);
        if (rows.isEmpty()) return;
        UUID warehouseId = rows.get(0).warehouseId();
        List<UUID> productIds = rows.stream().map(HoldRow::productId).distinct().toList();
        holdRepository.deleteByOwner(companyId, ownerType, ownerId);
        publish(companyId.getId(), warehouseId, productIds);
    }

    @Override
    @Transactional
    public void heartbeat(CompanyId companyId, StockHoldOwnerType ownerType, UUID ownerId, Duration ttl) {
        List<HoldRow> rows = holdRepository.findByOwner(companyId, ownerType, ownerId);
        if (rows.isEmpty()) return;
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl != null ? ttl : ttlFor(ownerType));
        for (HoldRow row : rows) {
            holdRepository.save(new HoldRow(
                    row.id(), row.companyId(), row.warehouseId(), row.productId(), row.quantity(),
                    row.holderId(), row.ownerType(), row.ownerId(),
                    expiresAt, row.createdAt(), now, row.version()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseAvailabilityRow> availableByWarehouse(CompanyId companyId,
                                                               UUID warehouseId,
                                                               StockHoldOwnerType excludeOwnerType,
                                                               UUID excludeOwnerId) {
        Instant now = Instant.now();
        WarehouseId wh = new WarehouseId(warehouseId);
        Map<UUID, BigDecimal[]> quant = holdRepository.sumOnHandAndReservedByWarehouse(companyId, wh);
        Map<UUID, BigDecimal> soft = holdRepository.sumActiveGroupedByProduct(
                companyId, wh, now, excludeOwnerType, excludeOwnerId);

        Set<UUID> products = new HashSet<>();
        products.addAll(quant.keySet());
        products.addAll(soft.keySet());

        List<WarehouseAvailabilityRow> out = new ArrayList<>();
        for (UUID productId : products) {
            BigDecimal[] qr = quant.getOrDefault(productId, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal onHand = qr[0];
            BigDecimal reserved = qr[1];
            BigDecimal softHeld = soft.getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal available = onHand.subtract(reserved).subtract(softHeld);
            if (available.signum() < 0) available = BigDecimal.ZERO;
            out.add(new WarehouseAvailabilityRow(productId, onHand, reserved, softHeld, available));
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal availableQuantity(CompanyId companyId,
                                        UUID warehouseId,
                                        UUID productId,
                                        StockHoldOwnerType excludeOwnerType,
                                        UUID excludeOwnerId) {
        Instant now = Instant.now();
        WarehouseId wh = new WarehouseId(warehouseId);
        BigDecimal free = holdRepository.sumFreeByWarehouse(companyId, wh, productId);
        BigDecimal soft = excludeOwnerType != null && excludeOwnerId != null
                ? holdRepository.sumActiveExcludingOwner(companyId, wh, productId, now, excludeOwnerType, excludeOwnerId)
                : holdRepository.sumActive(companyId, wh, productId, now);
        BigDecimal available = free.subtract(soft);
        return available.signum() < 0 ? BigDecimal.ZERO : available;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal softHeldExceptSalesOrder(CompanyId companyId,
                                               UUID warehouseId,
                                               UUID productId,
                                               UUID salesOrderId) {
        return holdRepository.sumActiveExceptSalesOrder(
                companyId, new WarehouseId(warehouseId), productId, Instant.now(), salesOrderId);
    }

    @Override
    @Transactional
    public void reduceSalesOrderHold(CompanyId companyId, UUID salesOrderId, UUID productId, BigDecimal qty) {
        if (qty == null || qty.signum() <= 0 || salesOrderId == null || productId == null) return;
        HoldRow row = holdRepository.findByOwnerAndProduct(
                companyId, StockHoldOwnerType.SALES_ORDER, salesOrderId, productId).orElse(null);
        if (row == null) return;
        BigDecimal next = row.quantity().subtract(qty);
        if (next.signum() <= 0) {
            holdRepository.delete(row);
        } else {
            holdRepository.save(new HoldRow(
                    row.id(), row.companyId(), row.warehouseId(), row.productId(), next,
                    row.holderId(), row.ownerType(), row.ownerId(),
                    row.expiresAt(), row.createdAt(), Instant.now(), row.version()));
        }
        publish(companyId.getId(), row.warehouseId(), List.of(productId));
    }

    @Override
    @Transactional
    public int purgeExpired() {
        return holdRepository.deleteExpired(Instant.now());
    }

    private Map<UUID, BigDecimal> aggregateStockableLines(CompanyId companyId, List<HoldLine> lines) {
        Map<UUID, BigDecimal> out = new HashMap<>();
        if (lines == null) return out;
        for (HoldLine line : lines) {
            if (line == null || line.productId() == null || line.quantity() == null) continue;
            if (line.quantity().signum() <= 0) continue;
            Product product = productRepository.findById(new ProductId(line.productId())).orElse(null);
            if (product == null) {
                throw new InventoryDomainException("error.inventory.productNotFound",
                        new Object[]{line.productId()}, "Product not found: " + line.productId());
            }
            if (product.getCompanyId() == null || !product.getCompanyId().equals(companyId)) {
                throw new InventoryDomainException("error.inventory.productWrongCompany", null, "Product company mismatch");
            }
            ProductType type = product.getProductType();
            if (type != ProductType.STOCKABLE && type != ProductType.CONSUMABLE) {
                continue;
            }
            out.merge(line.productId(), line.quantity(), BigDecimal::add);
        }
        return out;
    }

    private static Duration ttlFor(StockHoldOwnerType type) {
        return switch (type) {
            case CART -> CART_TTL;
            case DRAFT -> DRAFT_TTL;
            case SALES_ORDER -> SALES_ORDER_TTL;
        };
    }

    private void publish(UUID companyId, UUID warehouseId, List<UUID> productIds) {
        availabilityHub.publish(new AvailabilityChangedEvent(companyId, warehouseId, productIds));
    }
}
